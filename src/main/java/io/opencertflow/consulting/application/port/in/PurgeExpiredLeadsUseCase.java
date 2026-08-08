package io.opencertflow.consulting.application.port.in;

import reactor.core.publisher.Mono;

/** 보존 기간이 지난 종착(완료·취소) 상담 리드를 파기한다(F-BE-014). */
public interface PurgeExpiredLeadsUseCase {

    /** 파기를 수행하고 삭제된 리드 건수를 돌려준다. */
    Mono<Long> purgeExpired();
}
