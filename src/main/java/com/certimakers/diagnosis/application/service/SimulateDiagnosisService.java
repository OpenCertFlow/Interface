package com.certimakers.diagnosis.application.service;

import com.certimakers.common.application.annotation.UseCase;
import com.certimakers.common.application.support.BlockingBridge;
import com.certimakers.common.domain.error.BusinessException;
import com.certimakers.diagnosis.application.port.in.GetRemediationPlanQuery;
import com.certimakers.diagnosis.application.port.in.SimulateCommand;
import com.certimakers.diagnosis.application.port.in.SimulateDiagnosisUseCase;
import com.certimakers.diagnosis.application.port.out.LoadDiagnosisPort;
import com.certimakers.diagnosis.application.port.out.LoadRuleSetPort;
import com.certimakers.diagnosis.application.port.out.LoadScoreRubricPort;
import com.certimakers.diagnosis.domain.error.DiagnosisErrorCode;
import com.certimakers.diagnosis.domain.model.Diagnosis;
import com.certimakers.diagnosis.domain.model.DiagnosisId;
import com.certimakers.diagnosis.domain.model.ProductGroup;
import com.certimakers.diagnosis.domain.rule.RuleSet;
import com.certimakers.diagnosis.domain.service.ScoreRubric;
import com.certimakers.diagnosis.domain.simulation.DiagnosisSimulator;
import com.certimakers.diagnosis.domain.simulation.RemediationPlan;
import com.certimakers.diagnosis.domain.simulation.RemediationPlanner;
import com.certimakers.diagnosis.domain.simulation.SimulationOutcome;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

/**
 * 시뮬레이션·보완 계획 오케스트레이션.
 *
 * <p>진단 실행({@link DiagnoseProductService})과 달리 <b>RAG 검색도 LLM 문장화도 하지 않고,
 * 결과를 저장하지도 않는다.</b> 결정론 영역(①룰셋 로드 → ②룰 평가·점수 산정)만 다시 돌린다.
 * 덕분에 응답이 수 밀리초 안에 끝나 화면에서 토글을 움직일 때마다 즉시 반응할 수 있다.
 *
 * <p>저장하지 않는 것은 성능이 아니라 <b>기록의 무결성</b> 때문이다. 원본 진단은 특정 시점의 룰셋
 * 버전으로 확정된 기록이고, 가정이 그것을 덮어쓰면 "이 진단은 언제 무엇을 근거로 나왔는가"에
 * 답할 수 없게 된다.
 */
@UseCase
public class SimulateDiagnosisService implements SimulateDiagnosisUseCase, GetRemediationPlanQuery {

    private final DiagnosisSimulator simulator = new DiagnosisSimulator();
    private final RemediationPlanner planner = new RemediationPlanner();

    private final LoadDiagnosisPort loadDiagnosisPort;
    private final LoadRuleSetPort loadRuleSetPort;
    private final LoadScoreRubricPort loadScoreRubricPort;
    private final BlockingBridge blockingBridge;

    public SimulateDiagnosisService(
            LoadDiagnosisPort loadDiagnosisPort,
            LoadRuleSetPort loadRuleSetPort,
            LoadScoreRubricPort loadScoreRubricPort,
            BlockingBridge blockingBridge) {
        this.loadDiagnosisPort = loadDiagnosisPort;
        this.loadRuleSetPort = loadRuleSetPort;
        this.loadScoreRubricPort = loadScoreRubricPort;
        this.blockingBridge = blockingBridge;
    }

    @Override
    public Mono<SimulationOutcome> simulate(SimulateCommand command) {
        return loadEvaluated(command.diagnosisId())
                .flatMap(diagnosis -> {
                    ProductGroup group = diagnosis.profile().productGroup();
                    return Mono.zip(loadRuleSet(group), loadRubric(group))
                            .map(loaded -> simulate(diagnosis, command, loaded));
                });
    }

    @Override
    public Mono<RemediationPlan> plan(DiagnosisId diagnosisId, int targetScore) {
        return loadEvaluated(diagnosisId)
                .map(diagnosis -> planner.planFor(
                        new RemediationPlanner.ScoreResultView(
                                diagnosis.score(), diagnosis.checklist()),
                        targetScore));
    }

    private SimulationOutcome simulate(
            Diagnosis diagnosis, SimulateCommand command, Tuple2<RuleSet, ScoreRubric> loaded) {
        return simulator.simulate(
                diagnosis.profile(),
                diagnosis.score(),
                diagnosis.checklist(),
                diagnosis.candidates(),
                command.adjustment(),
                loaded.getT1(),
                loaded.getT2());
    }

    /**
     * 진단을 불러오되, 룰 평가가 끝나 점수·체크리스트가 확정된 것만 통과시킨다.
     *
     * <p>평가 전 상태에는 비교 기준이 없어 "무엇이 달라지는가"를 계산할 수 없다. 여기서 막지 않으면
     * 도메인 깊은 곳에서 null로 터진다.
     */
    private Mono<Diagnosis> loadEvaluated(DiagnosisId id) {
        return blockingBridge.mono(() -> loadDiagnosisPort.load(id))
                .switchIfEmpty(Mono.error(
                        new BusinessException(DiagnosisErrorCode.DIAGNOSIS_NOT_FOUND)))
                .flatMap(diagnosis -> diagnosis.score() == null
                        ? Mono.error(new BusinessException(DiagnosisErrorCode.SIMULATION_NOT_AVAILABLE))
                        : Mono.just(diagnosis));
    }

    private Mono<RuleSet> loadRuleSet(ProductGroup group) {
        return blockingBridge.mono(() -> loadRuleSetPort.loadActive(group))
                .switchIfEmpty(Mono.error(
                        new BusinessException(DiagnosisErrorCode.RULE_SET_NOT_FOUND)));
    }

    private Mono<ScoreRubric> loadRubric(ProductGroup group) {
        return blockingBridge.mono(() -> loadScoreRubricPort.load(group));
    }
}
