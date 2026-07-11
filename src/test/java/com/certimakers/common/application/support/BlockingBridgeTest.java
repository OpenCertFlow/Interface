package com.certimakers.common.application.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

class BlockingBridgeTest {

    private Scheduler scheduler;
    private BlockingBridge bridge;

    @BeforeEach
    void setUp() {
        scheduler = Schedulers.newBoundedElastic(2, 10, "test-jdbc");
        bridge = new BlockingBridge(scheduler);
    }

    @AfterEach
    void tearDown() {
        scheduler.dispose();
    }

    @Test
    @DisplayName("블로킹 호출을 구독 스레드가 아닌 전용 스케줄러 스레드에서 실행한다")
    void 전용_스케줄러_스레드에서_실행된다() {
        AtomicReference<String> executedOn = new AtomicReference<>();

        bridge.mono(() -> {
            executedOn.set(Thread.currentThread().getName());
            return "ok";
        }).block();

        // 이것이 ADR-0002의 전부다: JDBC는 이벤트 루프가 아닌 이 스레드에서 돈다.
        assertThat(executedOn.get()).startsWith("test-jdbc");
    }

    @Test
    @DisplayName("supplier가 null을 반환하면 빈 Mono가 된다 — switchIfEmpty로 다루라는 뜻")
    void null_반환은_빈_Mono가_된다() {
        StepVerifier.create(bridge.mono(() -> null).switchIfEmpty(Mono.just("fallback")))
                .expectNext("fallback")
                .verifyComplete();
    }

    @Test
    @DisplayName("supplier가 던진 예외는 그대로 전파된다")
    void 예외는_그대로_전파된다() {
        StepVerifier.create(bridge.mono(() -> {
                    throw new IllegalStateException("DB 연결 실패");
                }))
                .expectErrorMessage("DB 연결 실패")
                .verify();
    }

    @Test
    @DisplayName("컬렉션 조회를 Flux로 펼친다")
    void 컬렉션을_Flux로_펼친다() {
        StepVerifier.create(bridge.flux(() -> List.of("a", "b", "c")))
                .expectNext("a", "b", "c")
                .verifyComplete();
    }

    @Test
    @DisplayName("반환값 없는 쓰기 작업을 실행한다")
    void 반환값_없는_작업을_실행한다() {
        AtomicReference<Boolean> executed = new AtomicReference<>(false);

        StepVerifier.create(bridge.run(() -> executed.set(true))).verifyComplete();

        assertThat(executed.get()).isTrue();
    }
}
