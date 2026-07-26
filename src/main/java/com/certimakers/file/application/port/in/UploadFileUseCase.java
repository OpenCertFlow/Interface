package com.certimakers.file.application.port.in;

import com.certimakers.file.domain.model.StoredFile;
import com.certimakers.file.domain.model.Visibility;
import org.springframework.core.io.buffer.DataBuffer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** 파일 업로드. 바이트는 저장소로, 메타데이터는 DB로 나뉘어 들어간다. */
public interface UploadFileUseCase {

    Mono<StoredFile> upload(UploadCommand command);

    /**
     * @param originalFileName 사용자가 올린 원본 파일명. 저장 경로로 쓰이지 않는다
     * @param contentType      클라이언트가 보낸 MIME 타입. 신뢰하지 않고 힌트로만 쓴다
     * @param ownerId          업로더 식별자
     * @param visibility       공개 범위. 호출하는 쪽(웹 어댑터·다른 컨텍스트)이 정한다 —
     *                         클라이언트가 요청 값으로 스스로 선언하게 하지 않는다
     * @param content          파일 바이트 스트림. 메모리에 전부 올리지 않는다
     */
    record UploadCommand(
            String originalFileName,
            String contentType,
            String ownerId,
            Visibility visibility,
            Flux<DataBuffer> content) {
    }
}
