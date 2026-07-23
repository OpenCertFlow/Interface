package com.certimakers.file.application.port.in;

import com.certimakers.file.domain.model.StoredFile;
import org.springframework.core.io.buffer.DataBuffer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** 파일 다운로드. 메타데이터와 바이트 스트림을 함께 돌려준다. */
public interface DownloadFileQuery {

    Mono<Download> download(String fileId);

    /**
     * @param metadata 파일명·형식·크기. 응답 헤더를 만드는 데 쓴다
     * @param content  파일 바이트 스트림
     */
    record Download(StoredFile metadata, Flux<DataBuffer> content) {
    }
}
