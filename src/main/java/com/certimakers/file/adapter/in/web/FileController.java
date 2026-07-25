package com.certimakers.file.adapter.in.web;

import com.certimakers.common.adapter.in.web.annotation.WebAdapter;
import com.certimakers.common.adapter.in.web.response.ApiResponse;
import com.certimakers.common.adapter.in.web.trace.TraceId;
import com.certimakers.common.domain.port.TimeProvider;
import com.certimakers.file.application.port.in.DeleteFileUseCase;
import com.certimakers.file.application.port.in.DownloadFileQuery;
import com.certimakers.file.application.port.in.UploadFileUseCase;
import com.certimakers.file.domain.model.StoredFile;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 파일 업로드·다운로드 API.
 *
 * <p>업로드와 삭제는 인증이 필요하고, 다운로드는 열려 있다 — 게시글 첨부를 비로그인 사용자도 볼 수
 * 있어야 하기 때문이다. 식별자가 전역 시퀀스(순번)로 바뀌면서 값이 열거 가능해졌으므로, 접근 통제를
 * 식별자의 비추측성에 의존하지 않는다. 지금은 첨부 공개가 의도된 동작이며, 비공개 파일을 다루게 되면
 * 다운로드에도 소유·권한 검사를 걸어야 한다.
 */
@WebAdapter
@RequestMapping("/api/v1/files")
public class FileController {

    private final UploadFileUseCase uploadFileUseCase;
    private final DownloadFileQuery downloadFileQuery;
    private final DeleteFileUseCase deleteFileUseCase;
    private final TimeProvider timeProvider;

    public FileController(
            UploadFileUseCase uploadFileUseCase,
            DownloadFileQuery downloadFileQuery,
            DeleteFileUseCase deleteFileUseCase,
            TimeProvider timeProvider) {
        this.uploadFileUseCase = uploadFileUseCase;
        this.downloadFileQuery = downloadFileQuery;
        this.deleteFileUseCase = deleteFileUseCase;
        this.timeProvider = timeProvider;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ResponseEntity<ApiResponse<FileResponses.Uploaded>>> upload(
            @RequestPart("file") FilePart filePart,
            Mono<Principal> principal) {

        return principal.map(Principal::getName)
                .flatMap(ownerId -> uploadFileUseCase.upload(new UploadFileUseCase.UploadCommand(
                        filePart.filename(),
                        contentTypeOf(filePart),
                        ownerId,
                        filePart.content())))
                .map(FileResponses.Uploaded::from)
                .flatMap(body -> wrap(body, HttpStatus.CREATED));
    }

    /**
     * 파일 다운로드. 브라우저가 저장된 파일을 우리 도메인에서 <b>실행</b>하지 않도록 헤더를 세운다.
     *
     * <p>이미지·PDF만 인라인으로 열고 나머지는 첨부로 내려보낸다. HTML·SVG를 인라인으로 열어 주면
     * 업로드된 스크립트가 우리 출처에서 실행된다(저장형 XSS).
     */
    @GetMapping("/{id}")
    public Mono<ResponseEntity<Flux<DataBuffer>>> download(@PathVariable String id) {
        return downloadFileQuery.download(id)
                .map(download -> {
                    StoredFile metadata = download.metadata();
                    return ResponseEntity.ok()
                            .headers(headers -> applyDownloadHeaders(headers, metadata))
                            .body(download.content());
                });
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<ApiResponse<Void>>> delete(
            @PathVariable String id, Mono<Authentication> authentication) {

        return authentication
                .flatMap(auth -> deleteFileUseCase.delete(new DeleteFileUseCase.DeleteCommand(
                        id, auth.getName(), hasAdminRole(auth))))
                .then(wrap(null, HttpStatus.NO_CONTENT));
    }

    private void applyDownloadHeaders(HttpHeaders headers, StoredFile metadata) {
        headers.setContentType(MediaType.parseMediaType(metadata.contentType().value()));
        headers.setContentLength(metadata.sizeInBytes());

        String fileName = metadata.originalName().value();
        ContentDisposition disposition = metadata.contentType().safeToRenderInline()
                ? ContentDisposition.inline().filename(fileName, StandardCharsets.UTF_8).build()
                : ContentDisposition.attachment().filename(fileName, StandardCharsets.UTF_8).build();
        headers.setContentDisposition(disposition);

        // 브라우저의 형식 추측을 막는다. 추측이 허용되면 확장자를 속인 파일이 스크립트로 해석될 수 있다.
        headers.set("Content-Security-Policy", "default-src 'none'; sandbox");
        headers.set("X-Content-Type-Options", "nosniff");
    }

    private String contentTypeOf(FilePart filePart) {
        MediaType type = filePart.headers().getContentType();
        return type != null ? type.toString() : null;
    }

    private boolean hasAdminRole(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);
    }

    private <T> Mono<ResponseEntity<ApiResponse<T>>> wrap(T body, HttpStatus status) {
        return TraceId.current().map(traceId -> ResponseEntity.status(status)
                .body(ApiResponse.success(body, traceId, timeProvider.now())));
    }
}
