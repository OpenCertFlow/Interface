package com.certimakers.file.application.port.out;

import com.certimakers.file.domain.model.StorageKey;
import org.springframework.core.io.buffer.DataBuffer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 파일 바이트 저장소. 구현은 로컬 디스크이며, 오브젝트 스토리지(S3 등)로 갈아끼울 수 있다.
 *
 * <p>내용을 {@code byte[]}가 아니라 {@link DataBuffer} 스트림으로 주고받는다. 업로드 파일 전체를
 * 메모리에 올리면 큰 파일 몇 개로 힙이 무너진다 — 흘려보내야 한다.
 *
 * <p>{@code DataBuffer}는 스프링 코어 타입이지 웹 타입이 아니다. 애플리케이션 계층이 웹 어댑터에
 * 의존하지 않는다는 규칙은 그대로 지켜진다.
 */
public interface FileStoragePort {

    /**
     * 스트림을 저장소에 쓴다.
     *
     * @return 실제로 쓴 바이트 수
     */
    Mono<Long> write(StorageKey key, Flux<DataBuffer> content);

    /** 저장된 내용을 스트림으로 읽는다. */
    Flux<DataBuffer> read(StorageKey key);

    /** 저장된 내용을 지운다. 이미 없으면 조용히 넘어간다(멱등). */
    Mono<Void> delete(StorageKey key);
}
