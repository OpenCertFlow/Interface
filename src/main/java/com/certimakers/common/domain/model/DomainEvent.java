package com.certimakers.common.domain.model;

import java.time.Instant;

/** 애그리거트 내부에서 발생한, 다른 곳에 알릴 가치가 있는 사실. */
public interface DomainEvent {

    /** 이벤트가 발생한 시각. {@code Instant.now()}가 아니라 TimeProvider로 주입받은 값이어야 한다. */
    Instant occurredAt();

    /** 로그와 메시지 라우팅에 쓰이는 이름. 예: {@code diagnosis.completed} */
    String eventName();
}
