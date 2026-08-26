package io.opencertflow.diagnosis.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.micrometer.tagged.TaggedCircuitBreakerMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import io.opencertflow.common.alert.application.port.out.OpsAlertPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AI 워커 호출의 회로 차단기(#37). 워커가 <b>무응답</b>일 때 매 진단이 타임아웃(RAG 2초 +
 * LLM 5초)을 기다리지 않도록, 연속 실패를 감지해 즉시 폴백으로 전환하고 복구도 자동 감지한다.
 *
 * <p>관리자 수동 스위치(F-WADM-020)는 선제 차단용(재배포 등 사람이 미리 아는 상황)으로
 * 공존한다 — 스위치가 켜져 있으면 호출 자체가 없어 차단기는 트래픽을 보지 않는다.
 *
 * <p>빈은 레지스트리 하나만 만든다. 같은 타입 CircuitBreaker 빈을 여럿 두면 주입에 Qualifier가
 * 필요해지고 파라미터 이름이 바뀌면 조용히 어긋난다 — 사용처가 레지스트리에서 이름으로 꺼낸다.
 */
@Configuration
@EnableConfigurationProperties(AiResilienceProperties.class)
public class AiCircuitBreakerConfig {

    private static final Logger log = LoggerFactory.getLogger(AiCircuitBreakerConfig.class);

    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry(
            AiResilienceProperties properties, MeterRegistry meterRegistry, OpsAlertPort alertPort) {
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(
                CircuitBreakerConfig.custom()
                        .slidingWindowSize(properties.slidingWindowSize())
                        .minimumNumberOfCalls(properties.minimumNumberOfCalls())
                        .failureRateThreshold(properties.failureRateThreshold())
                        .waitDurationInOpenState(properties.waitDurationInOpen())
                        .permittedNumberOfCallsInHalfOpenState(properties.permittedCallsInHalfOpen())
                        .build());
        // 회로 상태·차단 건수가 /actuator/metrics의 resilience4j.circuitbreaker.*로 노출된다.
        TaggedCircuitBreakerMetrics.ofCircuitBreakerRegistry(registry).bindTo(meterRegistry);
        // 상태 전환은 장애의 시작·복구 신호다(#39). 요청별 폴백 로그와 달리 전환 시점에 딱 한 번
        // 남고, 운영자 알림으로도 보낸다 — 웹훅 미설정이면 NoOp이라 로그만 남는다.
        registry.getEventPublisher().onEntryAdded(entryAdded ->
                entryAdded.getAddedEntry().getEventPublisher().onStateTransition(event -> {
                    var transition = event.getStateTransition();
                    String detail = "%s: %s → %s".formatted(event.getCircuitBreakerName(),
                            transition.getFromState(), transition.getToState());
                    if (transition.getToState() == CircuitBreaker.State.OPEN) {
                        log.warn("회로 상태 전환: {}", detail);
                    } else {
                        log.info("회로 상태 전환: {}", detail);
                    }
                    alertPort.send("회로 상태 전환", detail);
                }));
        return registry;
    }
}
