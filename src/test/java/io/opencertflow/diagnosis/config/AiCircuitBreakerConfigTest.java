package io.opencertflow.diagnosis.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 회로 전환 알림의 필터링(#39)을 검증한다 — 알림은 "사람이 행동을 바꿀 사건"(장애 시작·복구)
 * 에만 가고, 재시험 사이클(OPEN↔HALF_OPEN)은 로그로만 남는다. 트래픽 있는 긴 장애에서
 * 30초마다 같은 알림이 반복되는 경보 피로를 막는 장치다.
 *
 * <p>전환은 {@code transitionTo*State()}로 수동 유발한다 — 실제 호출 흐름 없이 상태 기계만
 * 움직여 리스너의 판정을 격리해서 본다.
 */
class AiCircuitBreakerConfigTest {

    private final List<String> alerts = new ArrayList<>();

    private CircuitBreaker breakerWithWiring() {
        CircuitBreakerRegistry registry = new AiCircuitBreakerConfig().circuitBreakerRegistry(
                new AiResilienceProperties(10, 5, 50f, Duration.ofSeconds(30), 3),
                new SimpleMeterRegistry(),
                (title, message) -> alerts.add(message));
        return registry.circuitBreaker("ai-worker-search");
    }

    @Test
    @DisplayName("장애 시작(CLOSED→OPEN)과 복구(→CLOSED)에는 알림이 간다")
    void 사건에는_알림() {
        CircuitBreaker breaker = breakerWithWiring();

        breaker.transitionToOpenState();     // 장애 시작
        breaker.transitionToClosedState();   // 복구

        assertThat(alerts).hasSize(2);
        assertThat(alerts.get(0)).contains("CLOSED → OPEN");
        assertThat(alerts.get(1)).contains("CLOSED");
    }

    @Test
    @DisplayName("재시험 사이클(OPEN↔HALF_OPEN)은 알림을 보내지 않는다 — 긴 장애의 경보 피로 방지")
    void 심장박동은_무음() {
        CircuitBreaker breaker = breakerWithWiring();
        breaker.transitionToOpenState();
        alerts.clear();   // 장애 시작 알림은 별도 검증했으니 비움

        // 트래픽 있는 긴 장애에서 30초마다 반복되는 사이클
        breaker.transitionToHalfOpenState();   // 재시험 개시
        breaker.transitionToOpenState();       // 시험 실패, 재차단
        breaker.transitionToHalfOpenState();
        breaker.transitionToOpenState();

        assertThat(alerts).isEmpty();   // 새 정보가 없으니 알림도 없다
    }

    @Test
    @DisplayName("재시험 끝의 복구(HALF_OPEN→CLOSED)는 알림이 간다")
    void 재시험_복구는_알림() {
        CircuitBreaker breaker = breakerWithWiring();
        breaker.transitionToOpenState();
        breaker.transitionToHalfOpenState();
        alerts.clear();

        breaker.transitionToClosedState();   // 시험 성공 → 복구

        assertThat(alerts).hasSize(1);
        assertThat(alerts.get(0)).contains("HALF_OPEN → CLOSED");
    }
}
