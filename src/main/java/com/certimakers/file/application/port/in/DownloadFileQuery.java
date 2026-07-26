package com.certimakers.file.application.port.in;

import com.certimakers.file.domain.model.StoredFile;
import org.springframework.core.io.buffer.DataBuffer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** 파일 다운로드. 메타데이터와 바이트 스트림을 함께 돌려준다. */
public interface DownloadFileQuery {

    /**
     * @param requesterId      요청자 식별자. 비로그인이면 null — 공개 파일은 그래도 받아야 한다
     * @param requesterIsAdmin 관리자 여부
     */
    Mono<Download> download(String fileId, String requesterId, boolean requesterIsAdmin);

    /**
     * @param metadata 파일명·형식·크기. 응답 헤더를 만드는 데 쓴다
     * @param content  파일 바이트 스트림
     */
    record Download(StoredFile metadata, Flux<DataBuffer> content) {
    }
}
