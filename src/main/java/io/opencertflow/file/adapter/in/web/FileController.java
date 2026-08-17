package io.opencertflow.file.adapter.in.web;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import io.opencertflow.common.adapter.in.web.annotation.WebAdapter;
import io.opencertflow.common.adapter.in.web.response.ApiResponse;
import io.opencertflow.common.adapter.in.web.trace.TraceId;
import io.opencertflow.common.domain.port.TimeProvider;
import io.opencertflow.file.application.port.in.DeleteFileUseCase;
import io.opencertflow.file.application.port.in.DownloadFileQuery;
import io.opencertflow.file.application.port.in.UploadFileUseCase;
import io.opencertflow.file.domain.model.StoredFile;
import io.opencertflow.file.domain.model.Visibility;
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
 * <p>업로드와 삭제는 인증이 필요하고, 다운로드 경로 자체는 비로그인도 열려 있다 — 게시글 첨부를
 * 비로그인 사용자도 볼 수 있어야 하기 때문이다. 식별자가 전역 시퀀스(순번)로 바뀌면서 값이 열거
 * 가능해졌으므로, 접근 통제를 식별자의 비추측성에 의존하지 않는다. 실제 허용 여부는
 * {@link StoredFile#requireReadableBy}가 파일별 공개 범위(visibility)를 보고 판단한다 — 공개
 * 파일은 누구나, 비공개 파일은 소유자·관리자만 받는다.
 */
@Tag(name = "파일", description = "업로드 · 내려받기 · 삭제")
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
            @Parameter(hidden = true) Mono<Principal> principal) {

        return principal.map(Principal::getName)
                .flatMap(ownerId -> uploadFileUseCase.upload(new UploadFileUseCase.UploadCommand(
                        filePart.filename(),
                        contentTypeOf(filePart),
                        ownerId,
                        Visibility.PUBLIC,
                        filePart.content())))
                .map(FileResponses.Uploaded::from)
                .flatMap(body -> wrap(body, HttpStatus.CREATED));
    }

    /**
     * 파일 다운로드. 브라우저가 저장된 파일을 우리 도메인에서 <b>실행</b>하지 않도록 헤더를 세운다.
     *
     * <p>이미지·PDF만 인라인으로 열고 나머지는 첨부로 내려보낸다. HTML·SVG를 인라인으로 열어 주면
     * 업로드된 스크립트가 우리 출처에서 실행된다(저장형 XSS).
     *
     * <p>{@code authentication}이 비어 있으면(비로그인) 요청자 없이 조회한다 — 공개 파일은 그래도
     * 받아야 하고, 비공개 파일이면 이 시점에 {@link StoredFile#requireReadableBy}가 막는다.
     */
    @GetMapping("/{id}")
    public Mono<ResponseEntity<Flux<DataBuffer>>> download(
            @PathVariable String id, @Parameter(hidden = true) Mono<Authentication> authentication) {

        return authentication
                .flatMap(auth -> downloadFileQuery.download(id, auth.getName(), hasAdminRole(auth)))
                .switchIfEmpty(downloadFileQuery.download(id, null, false))
                .map(download -> {
                    StoredFile metadata = download.metadata();
                    return ResponseEntity.ok()
                            .headers(headers -> applyDownloadHeaders(headers, metadata))
                            .body(download.content());
                });
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<ApiResponse<Void>>> delete(
            @PathVariable String id, @Parameter(hidden = true) Mono<Authentication> authentication) {

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
