package com.certimakers.diagnosis.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.certimakers.common.application.support.BlockingBridge;
import com.certimakers.common.domain.error.BusinessException;
import com.certimakers.common.domain.port.IdGenerator;
import com.certimakers.common.domain.port.TimeProvider;
import com.certimakers.diagnosis.domain.ProductProfileFixtures;
import com.certimakers.diagnosis.domain.RuleSetFixtures;
import com.certimakers.diagnosis.application.DiagnosisPolicy;
import com.certimakers.diagnosis.application.port.in.DiagnoseCommand;
import com.certimakers.diagnosis.application.port.out.LoadRuleSetPort;
import com.certimakers.diagnosis.application.port.out.LoadScoreRubricPort;
import com.certimakers.diagnosis.application.port.out.NarrateReportPort;
import com.certimakers.diagnosis.application.port.out.SaveDiagnosisPort;
import com.certimakers.diagnosis.application.port.out.AiFallbackSwitchPort;
import com.certimakers.diagnosis.application.port.out.SearchEvidencePort;
import com.certimakers.diagnosis.domain.error.DiagnosisErrorCode;
import com.certimakers.diagnosis.domain.model.Diagnosis;
import com.certimakers.diagnosis.domain.model.DiagnosisStatus;
import com.certimakers.diagnosis.domain.model.Evidence;
import com.certimakers.diagnosis.domain.model.Narration;
import com.certimakers.diagnosis.domain.service.ScoreRubric;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.UUID;
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
            () -> UUID.fromString("018f0000-0000-7000-8000-000000000001");
    private final TimeProvider timeProvider =
            new com.certimakers.common.adapter.out.system.SystemTimeProvider(
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

    private DiagnoseProductService service(
            LoadRuleSetPort loadRuleSet,
            SearchEvidencePort search,
            NarrateReportPort narrate,
            SaveDiagnosisPort save) {
        return new DiagnoseProductService(
                loadRuleSet, rubricDefaults, search, narrate, save, fallbackOff,
                blockingBridge, idGenerator, timeProvider, policy);
    }

    private DiagnoseCommand dryerCommand() {
        return new DiagnoseCommand(ProductProfileFixtures.hairDryer(Set.of(RuleSetFixtures.TEST_REPORT)));
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
    @DisplayName("생성된 진단 ID는 IdGenerator에서 온다 — 도메인이 UUID를 직접 만들지 않는다")
    void 진단ID는_포트에서_온다() {
        DiagnoseProductService service = service(
                ruleSetFound(),
                query -> Mono.just(oneEvidence()),
                request -> Mono.just(llmNarration()),
                captureSave);

        StepVerifier.create(service.diagnose(dryerCommand()))
                .assertNext(diagnosis -> assertThat(diagnosis.id().value())
                        .isEqualTo(UUID.fromString("018f0000-0000-7000-8000-000000000001")))
                .verifyComplete();
    }
}
