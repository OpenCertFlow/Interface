package io.opencertflow.diagnosis.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import io.opencertflow.common.application.support.BlockingBridge;
import io.opencertflow.common.domain.error.BusinessException;
import io.opencertflow.common.domain.port.IdGenerator;
import io.opencertflow.common.domain.port.TimeProvider;
import io.opencertflow.diagnosis.domain.ProductProfileFixtures;
import io.opencertflow.diagnosis.domain.RuleSetFixtures;
import io.opencertflow.diagnosis.application.DiagnosisPolicy;
import io.opencertflow.diagnosis.application.port.in.DiagnoseCommand;
import io.opencertflow.diagnosis.application.port.out.LoadRuleSetPort;
import io.opencertflow.diagnosis.application.port.out.LoadScoreRubricPort;
import io.opencertflow.diagnosis.application.port.out.NarrateReportPort;
import io.opencertflow.diagnosis.application.port.out.SaveDiagnosisPort;
import io.opencertflow.diagnosis.application.port.out.AiFallbackSwitchPort;
import io.opencertflow.diagnosis.application.port.out.DiagnosisMetricsPort;
import io.opencertflow.diagnosis.application.port.out.SearchEvidencePort;
import io.opencertflow.diagnosis.domain.error.DiagnosisErrorCode;
import io.opencertflow.diagnosis.domain.model.Diagnosis;
import io.opencertflow.diagnosis.domain.model.DiagnosisStatus;
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
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

/**
 * 진단 오케스트레이션의 폴백 정책을 검증한다. 포트는 손으로 만든 페이크로 대체해 DB·워커 없이 돈다.
 * 검증의 핵심은 "③④단계 실패가 진단을 실패시키지 않는가"이다(03-diagnosis-flow.md).
 */
class DiagnoseProductServiceTest {

    private Scheduler scheduler;
    private BlockingBridge blockingBridge;
    private final IdGenerator idGenerator =
            () -> 1L;
    private final TimeProvider timeProvider =
            new io.opencertflow.common.adapter.out.system.SystemTimeProvider(
                    Clock.fixed(Instant.parse("2026-07-11T00:00:00Z"), ZoneOffset.UTC));
    private final DiagnosisPolicy policy =
            new DiagnosisPolicy(Duration.ofSeconds(2), Duration.ofSeconds(5));

    private final AtomicReference<Diagnosis> saved = new AtomicReference<>();

    @BeforeEach
    void setUp() {
        scheduler = Schedulers.newBoundedElastic(2, 10, "test-jdbc");
        blockingBridge = new BlockingBridge(scheduler);
        saved.set(null);
    }

    @AfterEach
    void tearDown() {
        scheduler.dispose();
    }

    // ── 페이크 포트 ────────────────────────────────────────────────

    private LoadRuleSetPort ruleSetFound() {
        return group -> RuleSetFixtures.smallApplianceV1();
    }

    private final LoadScoreRubricPort rubricDefaults = group -> ScoreRubric.defaultsOnly();

    private final SaveDiagnosisPort captureSave = diagnosis -> {
        saved.set(diagnosis);
        return diagnosis;
    };

    /** 폴백 스위치는 꺼짐(정상: RAG·LLM 호출). F-WADM-020 토글은 별도 검증한다. */
    private final AiFallbackSwitchPort fallbackOff = new AiFallbackSwitchPort() {
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

    /** 지표는 이 테스트의 관심사가 아니다. 호출돼도 아무 일도 하지 않는다. */
    private final DiagnosisMetricsPort noMetrics = new DiagnosisMetricsPort() {
        @Override public void diagnosisCompleted(Diagnosis diagnosis, java.time.Duration elapsed) {
        }
        @Override public void diagnosisFailed(String productGroup, String reason) {
        }
        @Override public void externalCall(String target, java.time.Duration elapsed, boolean ok) {
        }
    };

    private DiagnoseProductService service(
            LoadRuleSetPort loadRuleSet,
            SearchEvidencePort search,
            NarrateReportPort narrate,
            SaveDiagnosisPort save) {
        return new DiagnoseProductService(
                loadRuleSet, rubricDefaults, search, narrate, save, fallbackOff, noMetrics,
                blockingBridge, idGenerator, timeProvider, policy,
                // 기본 설정 레지스트리 — 최소 호출 수(100)를 채우지 않아 회로가 열리지 않는다.
                // 이 테스트의 관심사는 진단 흐름이지 차단기가 아니다.
                io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry.ofDefaults());
    }

    private DiagnoseCommand dryerCommand() {
        return DiagnoseCommand.anonymous(ProductProfileFixtures.hairDryer(Set.of(RuleSetFixtures.TEST_REPORT)));
    }

    private Narration llmNarration() {
        return new Narration("LLM 요약", List.of("행동"), List.of("질문"), "면책", "claude-opus-4-8", false);
    }

    private List<Evidence> oneEvidence() {
        return List.of(new Evidence("doc-1", "DOCUMENTS", "근거 문단",
                URI.create("https://example.kr/doc-1"), 0.85));
    }

    // ── 테스트 ────────────────────────────────────────────────────

    @Test
    @DisplayName("모두 성공하면 COMPLETED — 근거와 LLM 문장이 붙는다")
    void 정상흐름_COMPLETED() {
        DiagnoseProductService service = service(
                ruleSetFound(),
                query -> Mono.just(oneEvidence()),
                request -> Mono.just(llmNarration()),
                captureSave);

        StepVerifier.create(service.diagnose(dryerCommand()))
                .assertNext(diagnosis -> {
                    assertThat(diagnosis.status()).isEqualTo(DiagnosisStatus.COMPLETED);
                    assertThat(diagnosis.degraded().any()).isFalse();
                    assertThat(diagnosis.evidences()).hasSize(1);
                    assertThat(diagnosis.narration()).isPresent();
                    assertThat(diagnosis.candidates()).isNotEmpty();
                })
                .verifyComplete();

        assertThat(saved.get()).isNotNull(); // 저장까지 도달
    }

    @Test
    @DisplayName("RAG 검색이 실패해도 진단은 완료된다 — COMPLETED_DEGRADED, 점수는 유효")
    void RAG_실패시_저하완료() {
        DiagnoseProductService service = service(
                ruleSetFound(),
                query -> Mono.error(new RuntimeException("RAG down")),
                request -> Mono.just(llmNarration()),
                captureSave);

        StepVerifier.create(service.diagnose(dryerCommand()))
                .assertNext(diagnosis -> {
                    assertThat(diagnosis.status()).isEqualTo(DiagnosisStatus.COMPLETED_DEGRADED);
                    assertThat(diagnosis.degraded().isEvidenceDegraded()).isTrue();
                    assertThat(diagnosis.evidences()).isEmpty();
                    assertThat(diagnosis.score().applicable()).isTrue(); // 점수는 그대로
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("RAG 타임아웃도 실패로 처리되어 근거 없이 진행한다")
    void RAG_타임아웃시_저하완료() {
        DiagnoseProductService service = service(
                ruleSetFound(),
                query -> Mono.just(oneEvidence()).delayElement(Duration.ofSeconds(10)),
                request -> Mono.just(llmNarration()),
                captureSave);

        StepVerifier.create(service.diagnose(dryerCommand()))
                .assertNext(diagnosis ->
                        assertThat(diagnosis.degraded().isEvidenceDegraded()).isTrue())
                .verifyComplete();
    }

    @Test
    @DisplayName("LLM 문장화가 실패하면 템플릿 문장으로 대체한다 — COMPLETED_DEGRADED")
    void LLM_실패시_템플릿_폴백() {
        DiagnoseProductService service = service(
                ruleSetFound(),
                query -> Mono.just(oneEvidence()),
                request -> Mono.error(new RuntimeException("LLM timeout")),
                captureSave);

        StepVerifier.create(service.diagnose(dryerCommand()))
                .assertNext(diagnosis -> {
                    assertThat(diagnosis.status()).isEqualTo(DiagnosisStatus.COMPLETED_DEGRADED);
                    assertThat(diagnosis.degraded().isNarrationDegraded()).isTrue();
                    assertThat(diagnosis.narration()).isPresent();
                    assertThat(diagnosis.narration().get().isTemplateFallback()).isTrue();
                    // 근거는 정상이므로 evidence는 저하 아님
                    assertThat(diagnosis.degraded().isEvidenceDegraded()).isFalse();
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("룰셋이 없으면 폴백 없이 RULE_SET_NOT_FOUND(503)로 실패한다")
    void 룰셋_없으면_진단_실패() {
        LoadRuleSetPort noRuleSet = group -> null; // 활성 룰셋 없음
        DiagnoseProductService service = service(
                noRuleSet,
                query -> Mono.just(oneEvidence()),
                request -> Mono.just(llmNarration()),
                captureSave);

        StepVerifier.create(service.diagnose(dryerCommand()))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(BusinessException.class);
                    assertThat(((BusinessException) error).errorCode())
                            .isEqualTo(DiagnosisErrorCode.RULE_SET_NOT_FOUND);
                })
                .verify();

        assertThat(saved.get()).isNull(); // 저장까지 가지 않았다
    }

    @Test
    @DisplayName("검색은 성공했지만 근거가 0건이면 저하로 표시한다 — 근거란이 조용히 비지 않는다")
    void 근거_0건도_저하로_표시() {
        DiagnoseProductService service = service(
                ruleSetFound(),
                query -> Mono.just(List.of()), // 색인에 해당 제품군 문서가 없는 상황
                request -> Mono.just(llmNarration()),
                captureSave);

        StepVerifier.create(service.diagnose(dryerCommand()))
                .assertNext(diagnosis -> {
                    assertThat(diagnosis.evidences()).isEmpty();
                    assertThat(diagnosis.degraded().isEvidenceDegraded())
                            .as("근거를 붙이지 못했으면 이유와 무관하게 사용자에게 알려야 한다")
                            .isTrue();
                    assertThat(diagnosis.status()).isEqualTo(DiagnosisStatus.COMPLETED_DEGRADED);
                    assertThat(diagnosis.score().applicable())
                            .as("근거가 없어도 판정과 점수는 유효하다")
                            .isTrue();
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("후보가 없으면 검색 자체를 하지 않으므로 저하가 아니다")
    void 후보_없으면_저하_아님() {
        DiagnoseProductService service = service(
                group -> RuleSetFixtures.smallApplianceV1(),
                query -> Mono.error(new IllegalStateException("호출되면 안 된다")),
                request -> Mono.just(llmNarration()),
                captureSave);

        DiagnoseCommand noCandidate = DiagnoseCommand.anonymous(
                ProductProfileFixtures.nonElectricProduct());

        StepVerifier.create(service.diagnose(noCandidate))
                .assertNext(diagnosis -> {
                    assertThat(diagnosis.candidates()).isEmpty();
                    assertThat(diagnosis.degraded().isEvidenceDegraded()).isFalse();
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("생성된 진단 ID는 IdGenerator에서 온다 — 도메인이 Long를 직접 만들지 않는다")
    void 진단ID는_포트에서_온다() {
        DiagnoseProductService service = service(
                ruleSetFound(),
                query -> Mono.just(oneEvidence()),
                request -> Mono.just(llmNarration()),
                captureSave);

        StepVerifier.create(service.diagnose(dryerCommand()))
                .assertNext(diagnosis -> assertThat(diagnosis.id().value())
                        .isEqualTo(1L))
                .verifyComplete();
    }
}
