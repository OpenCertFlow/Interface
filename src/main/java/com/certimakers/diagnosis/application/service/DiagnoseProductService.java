package com.certimakers.diagnosis.application.service;

import com.certimakers.common.application.annotation.UseCase;
import com.certimakers.common.application.support.BlockingBridge;
import com.certimakers.common.domain.error.BusinessException;
import com.certimakers.common.domain.port.IdGenerator;
import com.certimakers.common.domain.port.TimeProvider;
import com.certimakers.diagnosis.application.DiagnosisPolicy;
import com.certimakers.diagnosis.application.port.in.DiagnoseCommand;
import com.certimakers.diagnosis.application.port.in.DiagnoseProductUseCase;
import com.certimakers.diagnosis.application.port.out.AiFallbackSwitchPort;
import com.certimakers.diagnosis.application.port.out.EvidenceQuery;
import com.certimakers.diagnosis.application.port.out.LoadRuleSetPort;
import com.certimakers.diagnosis.application.port.out.LoadScoreRubricPort;
import com.certimakers.diagnosis.application.port.out.NarrateReportPort;
import com.certimakers.diagnosis.application.port.out.NarrationRequest;
import com.certimakers.diagnosis.application.port.out.SaveDiagnosisPort;
import com.certimakers.diagnosis.application.port.out.SearchEvidencePort;
import com.certimakers.diagnosis.domain.error.DiagnosisErrorCode;
import com.certimakers.diagnosis.domain.model.Diagnosis;
import com.certimakers.diagnosis.domain.model.DiagnosisId;
import com.certimakers.diagnosis.domain.model.ProductGroup;
import com.certimakers.diagnosis.domain.model.ProductProfile;
import com.certimakers.diagnosis.domain.rule.RuleSet;
import com.certimakers.diagnosis.domain.service.RuleEvaluationResult;
import com.certimakers.diagnosis.domain.service.RuleEvaluator;
import com.certimakers.diagnosis.domain.service.ScoreCalculator;
import com.certimakers.diagnosis.domain.service.ScoreResult;
import com.certimakers.diagnosis.domain.service.ScoreRubric;
import com.certimakers.diagnosis.domain.service.TemplateNarrator;
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
    private final BlockingBridge blockingBridge;
    private final IdGenerator idGenerator;
    private final TimeProvider timeProvider;
    private final DiagnosisPolicy policy;

    public DiagnoseProductService(
            LoadRuleSetPort loadRuleSetPort,
            LoadScoreRubricPort loadScoreRubricPort,
            SearchEvidencePort searchEvidencePort,
            NarrateReportPort narrateReportPort,
            SaveDiagnosisPort saveDiagnosisPort,
            AiFallbackSwitchPort aiFallbackSwitch,
            BlockingBridge blockingBridge,
            IdGenerator idGenerator,
            TimeProvider timeProvider,
            DiagnosisPolicy policy) {
        this.loadRuleSetPort = loadRuleSetPort;
        this.loadScoreRubricPort = loadScoreRubricPort;
        this.searchEvidencePort = searchEvidencePort;
        this.narrateReportPort = narrateReportPort;
        this.saveDiagnosisPort = saveDiagnosisPort;
        this.aiFallbackSwitch = aiFallbackSwitch;
        this.blockingBridge = blockingBridge;
        this.idGenerator = idGenerator;
        this.timeProvider = timeProvider;
        this.policy = policy;
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
                .flatMap(this::persist);                           // ⑤ 저장 (폴백 없음)
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

    /** ③ 근거 검색. 후보가 없으면 검색할 것이 없고, 실패·타임아웃이면 근거 없이 진행한다. */
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
                .doOnNext(diagnosis::attachEvidences)
                .thenReturn(diagnosis)
                .onErrorResume(error -> {
                    log.warn("RAG 근거 검색 실패 — 근거 없이 진행합니다. diagnosisId={}, cause={}",
                            diagnosis.id().value(), error.toString());
                    diagnosis.markEvidenceDegraded();
                    return Mono.just(diagnosis);
                });
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
