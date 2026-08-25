package io.opencertflow.diagnosis.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.opencertflow.common.application.support.BlockingBridge;
import io.opencertflow.common.domain.port.IdGenerator;
import io.opencertflow.common.domain.port.TimeProvider;
import io.opencertflow.diagnosis.application.DiagnosisPolicy;
import io.opencertflow.diagnosis.application.port.in.DiagnoseCommand;
import io.opencertflow.diagnosis.application.port.out.AiFallbackSwitchPort;
import io.opencertflow.diagnosis.application.port.out.DiagnosisMetricsPort;
import io.opencertflow.diagnosis.application.port.out.LoadScoreRubricPort;
import io.opencertflow.diagnosis.application.port.out.NarrateReportPort;
import io.opencertflow.diagnosis.application.port.out.SaveDiagnosisPort;
import io.opencertflow.diagnosis.application.port.out.SearchEvidencePort;
import io.opencertflow.diagnosis.domain.ProductProfileFixtures;
import io.opencertflow.diagnosis.domain.RuleSetFixtures;
import io.opencertflow.diagnosis.domain.model.Diagnosis;
import io.opencertflow.diagnosis.domain.model.Evidence;
import io.opencertflow.diagnosis.domain.model.Narration;
import io.opencertflow.diagnosis.domain.service.ScoreRubric;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

/**
 * AI 워커 회로 차단기(#37)의 상태 전환을 검증한다.
 *
 * <p>{@code DiagnoseProductServiceTest}가 폴백 정책을 검증한다면, 여기는 차단기가 그 정책
 * 위에서 "부르지 않기"와 "자동 복구"를 해내는지를 본다. 시험을 빠르게 하려고 임계를 크게
 * 줄인 전용 레지스트리를 쓴다(최소 호출 2건, 차단 100ms, 시험 1건).
 *
 * <p>문장화(LLM) 페이크는 항상 성공한다 — RAG 차단기만 격리해서 본다. 두 차단기는 이름이
 * 달라 상태를 공유하지 않는다.
 */
class AiCircuitBreakerTest {

    /** 열림 유지 검증용 — 테스트가 끝나기 전에 HALF_OPEN으로 안 넘어가게 충분히 길게. */
    private static final Duration STAY_OPEN = Duration.ofSeconds(10);
    /** 자동 복구 검증용 — 기다릴 수 있을 만큼 짧게. */
    private static final Duration RECOVER_FAST = Duration.ofMillis(100);

    private Scheduler scheduler;
    private BlockingBridge blockingBridge;
    private CircuitBreakerRegistry registry;
    private DiagnoseProductService service;

    /** 검색 포트가 실제로 호출된 횟수 — "열린 회로는 부르지 않는다"의 증거. */
    private final AtomicInteger searchCalls = new AtomicInteger();
    /** true면 검색이 성공한다 — 워커 복구를 흉내 낸다. */
    private final AtomicBoolean workerHealthy = new AtomicBoolean(false);

    private final IdGenerator idGenerator = new IdGenerator() {
        private long next = 1;
        @Override public synchronized Long nextId() {
            return next++;
        }
    };
    private final TimeProvider timeProvider =
            new io.opencertflow.common.adapter.out.system.SystemTimeProvider(
                    Clock.fixed(Instant.parse("2026-07-11T00:00:00Z"), ZoneOffset.UTC));
    private final DiagnosisPolicy policy =
            new DiagnosisPolicy(Duration.ofSeconds(2), Duration.ofSeconds(5));

    @BeforeEach
    void setUp() {
        scheduler = Schedulers.newBoundedElastic(2, 10, "test-jdbc");
        blockingBridge = new BlockingBridge(scheduler);
        searchCalls.set(0);
        workerHealthy.set(false);
    }

    /**
     * 차단 대기 시간을 테스트가 고른다. "열림이 유지되는가"를 보는 테스트가 짧은 대기를 쓰면
     * 검증 도중 회로가 HALF_OPEN으로 넘어가 시험 호출이 통과된다 — 자동 복구 기능이
     * 테스트를 방해하는 셈이라, 열림 검증은 길게(10초), 복구 검증만 짧게(100ms) 둔다.
     */
    private void init(Duration openWait) {
        registry = CircuitBreakerRegistry.of(CircuitBreakerConfig.custom()
                .slidingWindowSize(4)
                .minimumNumberOfCalls(2)      // 2건 실패면 판단 시작
                .failureRateThreshold(50)
                .waitDurationInOpenState(openWait)
                .permittedNumberOfCallsInHalfOpenState(1)
                .build());
        service = new DiagnoseProductService(
                group -> RuleSetFixtures.smallApplianceV1(),
                (LoadScoreRubricPort) group -> ScoreRubric.defaultsOnly(),
                countingSearch(),
                narrateOk(),
                (SaveDiagnosisPort) diagnosis -> diagnosis,
                fallbackOff(),
                noMetrics(),
                blockingBridge, idGenerator, timeProvider, policy, registry);
    }

    @AfterEach
    void tearDown() {
        scheduler.dispose();
    }

    // ── 페이크 ─────────────────────────────────────────────────────

    private SearchEvidencePort countingSearch() {
        // Mono.defer가 핵심이다 — 카운트는 "조립"이 아니라 "구독" 시점에 올라가야 한다.
        // 차단기가 막는 것이 구독이므로, 조립 시점에 세면 차단돼도 카운트가 올라가
        // "부르지 않았다"를 검증할 수 없다. (진짜 어댑터도 구독 시점에야 HTTP를 쏜다)
        return query -> Mono.defer(() -> {
            searchCalls.incrementAndGet();
            if (workerHealthy.get()) {
                return Mono.just(List.of(new Evidence("doc-1", "DOCUMENTS", "근거 문단",
                        URI.create("https://example.kr/doc-1"), 0.85)));
            }
            return Mono.error(new RuntimeException("워커 무응답 흉내"));
        });
    }

    private NarrateReportPort narrateOk() {
        return request -> Mono.just(
                new Narration("요약", List.of("행동"), List.of("질문"), "면책", "test-llm", false));
    }

    private AiFallbackSwitchPort fallbackOff() {
        return new AiFallbackSwitchPort() {
            @Override public boolean isEvidenceDisabled() {
                return false;
            }
            @Override public boolean isNarrationDisabled() {
                return false;
            }
            @Override public void setEvidenceDisabled(boolean disabled) {
            }
            @Override public void setNarrationDisabled(boolean disabled) {
            }
        };
    }

    private DiagnosisMetricsPort noMetrics() {
        return new DiagnosisMetricsPort() {
            @Override public void diagnosisCompleted(Diagnosis diagnosis, Duration elapsed) {
            }
            @Override public void diagnosisFailed(String productGroup, String reason) {
            }
            @Override public void externalCall(String target, Duration elapsed, boolean ok) {
            }
        };
    }

    private Diagnosis diagnose() {
        return service.diagnose(DiagnoseCommand.anonymous(
                ProductProfileFixtures.hairDryer(Set.of(RuleSetFixtures.TEST_REPORT))))
                .block(Duration.ofSeconds(10));
    }

    private CircuitBreaker ragBreaker() {
        return registry.circuitBreaker("ai-worker-search");
    }

    // ── 검증 ─────────────────────────────────────────────────────

    @Test
    @DisplayName("연속 실패가 최소 호출 수를 채우면 회로가 열린다")
    void 연속_실패하면_회로가_열린다() {
        init(STAY_OPEN);
        diagnose();
        diagnose();   // 실패 2/2 = 100% ≥ 50%

        assertThat(ragBreaker().getState()).isEqualTo(CircuitBreaker.State.OPEN);
    }

    @Test
    @DisplayName("열린 회로는 포트를 부르지 않는다 — 이 기능의 존재 이유")
    void 열린_회로는_포트를_부르지_않는다() {
        init(STAY_OPEN);
        diagnose();
        diagnose();   // 회로 열림
        searchCalls.set(0);

        diagnose();
        diagnose();
        diagnose();

        assertThat(searchCalls.get()).isZero();   // 죽은 워커를 다시 확인하지 않았다
    }

    @Test
    @DisplayName("차단 중에도 진단은 실패하지 않는다 — 기존 폴백이 차단 예외를 받는다")
    void 차단_중에도_진단은_성공한다() {
        init(STAY_OPEN);
        diagnose();
        diagnose();   // 회로 열림

        Diagnosis diagnosis = diagnose();

        assertThat(diagnosis.degraded().isEvidenceDegraded()).isTrue();   // 근거만 빠지고
        assertThat(diagnosis.score()).isNotNull();                        // 판정·점수는 유효
    }

    @Test
    @DisplayName("대기 후 시험 호출이 성공하면 회로가 스스로 닫힌다 — 관리자 개입 불필요")
    void 워커가_복구되면_자동으로_닫힌다() throws InterruptedException {
        init(RECOVER_FAST);
        diagnose();
        diagnose();   // 회로 열림
        workerHealthy.set(true);          // 워커 복구
        Thread.sleep(RECOVER_FAST.toMillis() + 50);   // 차단 대기 경과 → HALF_OPEN 진입 가능

        Diagnosis probe = diagnose();     // 시험 호출 1건 통과 → 성공

        assertThat(ragBreaker().getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        assertThat(probe.degraded().isEvidenceDegraded()).isFalse();   // 근거가 다시 붙는다
        assertThat(probe.evidences()).isNotEmpty();
    }
}
