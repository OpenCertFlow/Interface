package com.certimakers.common.adapter.in.web.trace;

import reactor.core.publisher.Mono;
import reactor.util.context.ContextView;

/**
 * 요청 추적 ID. Reactor Context에 실려 리액티브 체인 전체를 따라다닌다.
 *
 * <p>WebFlux에서는 요청이 스레드를 갈아타므로 {@code ThreadLocal}(MDC)이 신뢰할 수 없다.
 * Reactor Context가 유일하게 올바른 저장소다.
 */
public final class TraceId {

    public static final String HEADER = "X-Trace-Id";
    public static final String CONTEXT_KEY = "traceId";
    public static final String UNKNOWN = "unknown";

    private TraceId() {
    }

    /** 현재 리액티브 체인의 traceId를 읽는다. 없으면 {@link #UNKNOWN}. */
    public static Mono<String> current() {
        return Mono.deferContextual(context -> Mono.just(from(context)));
    }

    public static String from(ContextView context) {
        return context.getOrDefault(CONTEXT_KEY, UNKNOWN);
    }
}
