package com.certimakers.common.application.support;

import java.util.Collection;
import java.util.Objects;
import java.util.function.Supplier;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

/**
 * 블로킹 호출(JPA/JDBC)을 리액티브 체인 안으로 안전하게 들여오는 <b>유일한</b> 통로.
 *
 * <p>WebFlux 위에서 JPA를 쓰기로 한 결정(ADR-0002)의 대가는 규율이다. 영속성 어댑터를 이벤트
 * 루프에서 직접 호출하면 Netty 워커 스레드가 JDBC I/O에 묶이고, 부하가 낮은 시연 환경에서는
 * 증상이 보이지 않는다. 이 클래스를 통과하면 호출은 {@code jdbcScheduler}의 전용 스레드로 옮겨진다.
 *
 * <p>사용 예:
 * <pre>{@code
 * return blockingBridge.mono(() -> loadRuleSetPort.loadActive(productGroup))
 *         .switchIfEmpty(Mono.error(new BusinessException(SERVICE_UNAVAILABLE)))
 *         .map(ruleSet -> ruleEvaluator.evaluate(profile, ruleSet));
 * }</pre>
 *
 * <p>테스트 프로파일의 BlockHound가 이 통로를 우회한 호출을 런타임에 잡는다.
 */
public class BlockingBridge {

    private final Scheduler scheduler;

    public BlockingBridge(Scheduler scheduler) {
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
    }

    /**
     * 블로킹 조회를 {@code Mono}로 감싼다.
     *
     * <p>{@code supplier}가 {@code null}을 반환하면 빈 {@code Mono}가 된다. 조회 실패를
     * {@code switchIfEmpty}로 다루라는 의미다. 예외는 그대로 전파되어 {@code onErrorResume}으로 잡힌다.
     */
    public <T> Mono<T> mono(Supplier<T> supplier) {
        return Mono.fromSupplier(supplier).subscribeOn(scheduler);
    }

    /** 블로킹 컬렉션 조회를 {@code Flux}로 펼친다. */
    public <T> Flux<T> flux(Supplier<? extends Collection<T>> supplier) {
        return Mono.fromSupplier(supplier).flatMapIterable(items -> items).subscribeOn(scheduler);
    }

    /** 반환값 없는 블로킹 쓰기(삭제 등). */
    public Mono<Void> run(Runnable action) {
        return Mono.<Void>fromRunnable(action).subscribeOn(scheduler);
    }
}
