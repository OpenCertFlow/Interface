package io.opencertflow.diagnosis.application.service;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.opencertflow.common.application.annotation.UseCase;
import io.opencertflow.common.application.support.BlockingBridge;
import io.opencertflow.common.domain.error.BusinessException;
import io.opencertflow.common.domain.port.IdGenerator;
import io.opencertflow.common.domain.port.TimeProvider;
import io.opencertflow.diagnosis.application.DiagnosisPolicy;
import io.opencertflow.diagnosis.application.port.in.DiagnoseCommand;
import io.opencertflow.diagnosis.application.port.in.DiagnoseProductUseCase;
import io.opencertflow.diagnosis.application.port.out.AiFallbackSwitchPort;
import io.opencertflow.diagnosis.application.port.out.DiagnosisMetricsPort;
import io.opencertflow.diagnosis.application.port.out.EvidenceQuery;
import io.opencertflow.diagnosis.application.port.out.LoadRuleSetPort;
import io.opencertflow.diagnosis.application.port.out.LoadScoreRubricPort;
import io.opencertflow.diagnosis.application.port.out.NarrateReportPort;
import io.opencertflow.diagnosis.application.port.out.NarrationRequest;
import io.opencertflow.diagnosis.application.port.out.SaveDiagnosisPort;
import io.opencertflow.diagnosis.application.port.out.SearchEvidencePort;
import io.opencertflow.diagnosis.domain.error.DiagnosisErrorCode;
import io.opencertflow.diagnosis.domain.model.Diagnosis;
import io.opencertflow.diagnosis.domain.model.DiagnosisId;
import io.opencertflow.diagnosis.domain.model.ProductGroup;
import io.opencertflow.diagnosis.domain.model.ProductProfile;
import io.opencertflow.diagnosis.domain.rule.RuleSet;
import io.opencertflow.diagnosis.domain.service.RuleEvaluationResult;
import io.opencertflow.diagnosis.domain.service.RuleEvaluator;
import io.opencertflow.diagnosis.domain.service.ScoreCalculator;
import io.opencertflow.diagnosis.domain.service.ScoreResult;
import io.opencertflow.diagnosis.domain.service.ScoreRubric;
import io.opencertflow.diagnosis.domain.service.TemplateNarrator;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

/**
 * 진단 오케스트레이션. 03-diagnosis-flow.md의 흐름을 코드로 옮긴 것이다.
 *
 * <p>다섯 단계: ① 룰셋·기준표 로드(블로킹) → ② 룰 평가·점수 산정(결정론, 순수 함수) →
 * ③ 근거 검색(폴백 가능) → ④ 문장화(폴백 가능) → ⑤ 저장(블로킹). ②가 끝나면 판정은 확정되며,
 * ③④는 그 결과에 근거와 설명을 덧붙일 뿐 판정을 바꾸지 못한다(ADR-0003).
 *
 * <p>결정론 도메인 서비스(RuleEvaluator·ScoreCalculator·TemplateNarrator)는 스프링 빈이 아니라
 * 이 클래스가 직접 생성해 보유한다 — 상태 없는 순수 함수라 주입할 이유가 없고, 도메인이 스프링을
 * 참조하면 ArchUnit이 막는다.
 */
@UseCase
public class DiagnoseProductService implements DiagnoseProductUseCase {

    private static final Logger log = LoggerFactory.getLogger(DiagnoseProductService.class);

    private final RuleEvaluator ruleEvaluator = new RuleEvaluator();
    private final ScoreCalculator scoreCalculator = new ScoreCalculator();
    private final TemplateNarrator templateNarrator = new TemplateNarrator();

    private final LoadRuleSetPort loadRuleSetPort;
    private final LoadScoreRubricPort loadScoreRubricPort;
    private final SearchEvidencePort searchEvidencePort;
    private final NarrateReportPort narrateReportPort;
    private final SaveDiagnosisPort saveDiagnosisPort;
    private final AiFallbackSwitchPort aiFallbackSwitch;
    private final DiagnosisMetricsPort metrics;
    private final BlockingBridge blockingBridge;
    private final IdGenerator idGenerator;
    private final TimeProvider timeProvider;
    private final DiagnosisPolicy policy;
    /** 워커가 무응답일 때 매 진단이 타임아웃을 기다리지 않게 한다(#37). RAG·LLM은 별개 장애라 분리. */
    private final CircuitBreaker ragBreaker;
    private final CircuitBreaker llmBreaker;

    public DiagnoseProductService(
            LoadRuleSetPort loadRuleSetPort,
            LoadScoreRubricPort loadScoreRubricPort,
            SearchEvidencePort searchEvidencePort,
            NarrateReportPort narrateReportPort,
            SaveDiagnosisPort saveDiagnosisPort,
            AiFallbackSwitchPort aiFallbackSwitch,
            DiagnosisMetricsPort metrics,
            BlockingBridge blockingBridge,
            IdGenerator idGenerator,
            TimeProvider timeProvider,
            DiagnosisPolicy policy,
            CircuitBreakerRegistry circuitBreakerRegistry) {
        this.loadRuleSetPort = loadRuleSetPort;
        this.loadScoreRubricPort = loadScoreRubricPort;
        this.searchEvidencePort = searchEvidencePort;
        this.narrateReportPort = narrateReportPort;
        this.saveDiagnosisPort = saveDiagnosisPort;
        this.aiFallbackSwitch = aiFallbackSwitch;
        this.metrics = metrics;
        this.blockingBridge = blockingBridge;
        this.idGenerator = idGenerator;
        this.timeProvider = timeProvider;
        this.policy = policy;
        this.ragBreaker = circuitBreakerRegistry.circuitBreaker("ai-worker-search");
        this.llmBreaker = circuitBreakerRegistry.circuitBreaker("ai-worker-narrate");
    }

    @Override
    public Mono<Diagnosis> diagnose(DiagnoseCommand command) {
        ProductProfile profile = command.profile();
        ProductGroup group = profile.productGroup();

        // ① 룰셋과 기준표를 블로킹 스케줄러에서 로드한다. 룰셋이 없으면 폴백 없이 503.
        Mono<RuleSet> ruleSetMono = blockingBridge
                .mono(() -> loadRuleSetPort.loadActive(group))
                .switchIfEmpty(Mono.error(
                        new BusinessException(DiagnosisErrorCode.RULE_SET_NOT_FOUND)));
        Mono<ScoreRubric> rubricMono = blockingBridge.mono(() -> loadScoreRubricPort.load(group));

        return Mono.zip(ruleSetMono, rubricMono)
                .map(loaded -> evaluate(command, loaded)) // ② 판정 확정
                .flatMap(this::attachEvidence)                     // ③ 근거 (폴백)
                .flatMap(this::attachNarration)                    // ④ 문장화 (폴백)
                .map(this::complete)
                .flatMap(this::persist)                            // ⑤ 저장 (폴백 없음)
                // 지표는 흐름 밖에서 붙인다. 재는 일이 진단을 바꾸지 않아야 한다.
                .elapsed()
                .doOnNext(timed -> metrics.diagnosisCompleted(
                        timed.getT2(), Duration.ofMillis(timed.getT1())))
                .map(reactor.util.function.Tuple2::getT2)
                .doOnError(error -> metrics.diagnosisFailed(
                        group.name(), error.getClass().getSimpleName()));
    }

    /** ② 룰 평가와 점수 산정. 순수 함수. 이 시점에 판정이 확정된다. */
    private Diagnosis evaluate(
            DiagnoseCommand command, Tuple2<RuleSet, ScoreRubric> loaded) {
        ProductProfile profile = command.profile();
        RuleSet ruleSet = loaded.getT1();
        ScoreRubric rubric = loaded.getT2();

        RuleEvaluationResult ruleResult = ruleEvaluator.evaluate(profile, ruleSet);
        ScoreResult scoreResult = scoreCalculator.calculate(
                ruleResult.requiredDocuments(),
                profile.heldDocuments(),
                profile.unknownDocuments(),
                rubric);

        Diagnosis diagnosis = Diagnosis.request(
                DiagnosisId.of(idGenerator.nextId()), profile, command.ownerUserId(), command.previousDiagnosisId(), timeProvider.now());
        diagnosis.evaluated(ruleResult, scoreResult);
        return diagnosis;
    }

    /**
     * ③ 근거 검색. 후보가 없으면 검색할 것이 없고, 실패·타임아웃이면 근거 없이 진행한다.
     *
     * <p><b>검색이 성공했는데 근거가 0건인 경우도 저하로 본다.</b> 사용자 입장에서는 워커가 죽어
     * 근거가 없는 것과 색인에 그 제품군 문서가 없어 근거가 없는 것이 구별되지 않는다. 둘 다
     * "공식 근거를 붙이지 못한 리포트"이므로 화면에 그 사실을 알려야 한다 — 근거란이 조용히
     * 비어 있으면 사용자는 "근거가 필요 없는 제품"으로 오해한다(기획서 2.4).
     */
    private Mono<Diagnosis> attachEvidence(Diagnosis diagnosis) {
        if (diagnosis.candidates().isEmpty()) {
            return Mono.just(diagnosis);
        }
        // 관리자가 폴백을 켜면 RAG를 호출하지 않고 곧바로 근거 없이 진행한다(타임아웃 대기 회피).
        if (aiFallbackSwitch.isEvidenceDisabled()) {
            diagnosis.markEvidenceDegraded();
            return Mono.just(diagnosis);
        }
        return searchEvidencePort.search(EvidenceQuery.from(diagnosis))
                .timeout(policy.searchTimeout())
                // 타임아웃 다음이어야 한다 — 타임아웃 에러가 차단기를 통과하며 실패로 집계된다.
                // 회로가 열리면 search()를 구독조차 않고 예외를 내며, 아래 폴백이 그대로 받는다.
                .transformDeferred(CircuitBreakerOperator.of(ragBreaker))
                .doOnNext(diagnosis::attachEvidences)
                .thenReturn(diagnosis)
                .onErrorResume(error -> {
                    log.warn("RAG 근거 검색 실패 — 근거 없이 진행합니다. diagnosisId={}, cause={}",
                            diagnosis.id().value(), error.toString());
                    diagnosis.markEvidenceDegraded();
                    return Mono.just(diagnosis);
                })
                .doOnNext(this::markDegradedWhenNoEvidence);
    }

    private void markDegradedWhenNoEvidence(Diagnosis diagnosis) {
        if (diagnosis.evidences().isEmpty() && !diagnosis.degraded().isEvidenceDegraded()) {
            log.info("근거 0건 — 해당 제품군의 공식 문서 색인을 확인하세요. diagnosisId={}, group={}",
                    diagnosis.id().value(), diagnosis.profile().productGroup());
            diagnosis.markEvidenceDegraded();
        }
    }

    /** ④ 문장화. 실패·타임아웃이면 템플릿 문장으로 대체한다. */
    private Mono<Diagnosis> attachNarration(Diagnosis diagnosis) {
        // 폴백이 켜져 있으면 LLM을 호출하지 않고 템플릿 문장으로 대체한다.
        if (aiFallbackSwitch.isNarrationDisabled()) {
            diagnosis.attachNarration(templateNarrator.narrate(diagnosis));
            return Mono.just(diagnosis);
        }
        return narrateReportPort.narrate(NarrationRequest.from(diagnosis))
                .timeout(policy.narrateTimeout())
                .transformDeferred(CircuitBreakerOperator.of(llmBreaker))
                .doOnNext(diagnosis::attachNarration)
                .thenReturn(diagnosis)
                .onErrorResume(error -> {
                    log.warn("LLM 문장화 실패 — 템플릿 문장으로 대체합니다. diagnosisId={}, cause={}",
                            diagnosis.id().value(), error.toString());
                    diagnosis.attachNarration(templateNarrator.narrate(diagnosis));
                    return Mono.just(diagnosis);
                });
    }

    private Diagnosis complete(Diagnosis diagnosis) {
        diagnosis.complete();
        return diagnosis;
    }

    /** ⑤ 저장. 폴백이 없다 — 실패하면 진단이 실패한다. */
    private Mono<Diagnosis> persist(Diagnosis diagnosis) {
        return blockingBridge.mono(() -> saveDiagnosisPort.save(diagnosis))
                .switchIfEmpty(Mono.error(
                        new BusinessException(DiagnosisErrorCode.DIAGNOSIS_SAVE_FAILED)));
    }
}
