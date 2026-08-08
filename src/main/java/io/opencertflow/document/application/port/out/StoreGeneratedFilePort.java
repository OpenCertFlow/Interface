package io.opencertflow.document.application.port.out;

import reactor.core.publisher.Mono;

/**
 * 생성된 PDF를 파일 컨텍스트에 저장한다.
 *
 * <p>문서 컨텍스트가 저장 로직을 다시 구현하지 않고 파일 컨텍스트에 위임한다 — 저장 경로 규칙과
 * 보안 검증이 두 벌 존재하면 한쪽만 고쳐질 위험이 있다.
 */
public interface StoreGeneratedFilePort {

    /** @return 저장된 파일의 식별자 */
    Mono<Long> store(String fileName, byte[] content, String ownerId);
}
