package com.certimakers.diagnosis.domain.simulation;

import com.certimakers.common.domain.model.Guard;
import com.certimakers.diagnosis.domain.model.CertificationCandidate;
import com.certimakers.diagnosis.domain.model.DocumentCode;
import com.certimakers.diagnosis.domain.model.ProductProfile;
import com.certimakers.diagnosis.domain.service.RuleEvaluationResult;
import com.certimakers.diagnosis.domain.service.ScoreResult;
import java.util.List;

/**
 * 시뮬레이션 결과. 가정을 적용해 다시 평가한 결과 <b>전체</b>와, 원본 대비 <b>무엇이 달라졌는지</b>를
 * 함께 담는다.
 *
 * <p>결과 전체를 담는 이유는 사용자가 "그래서 바뀐 상태가 어떤 모습인가"를 봐야 하기 때문이고,
 * 델타를 따로 담는 이유는 "무엇 때문에 달라졌는가"가 이 기능의 핵심 가치이기 때문이다.
 *
 * <p>이 결과는 <b>저장되지 않는다.</b> 시뮬레이션은 가정일 뿐이고, 원본 진단은 특정 시점의 룰셋
 * 버전으로 확정된 기록이므로 가정 때문에 덮어써지면 안 된다.
 *
 * @param adjustedProfile            가정을 적용한 제품 프로파일
 * @param ruleResult                 가정 프로파일에 대한 룰 평가 결과
 * @param scoreResult                가정 프로파일에 대한 점수 산정 결과
 * @param scoreDelta                 준비도 점수 변화
 * @param addedCandidates            새로 생긴 인증 후보
 * @param removedCandidates          더 이상 해당하지 않는 인증 후보
 * @param newlyRequiredDocuments     새로 요구된 서류
 * @param noLongerRequiredDocuments  더 이상 요구되지 않는 서류
 * @param newlySatisfiedDocuments    이번 가정으로 충족된 서류
 */
public record SimulationOutcome(
        ProductProfile adjustedProfile,
        RuleEvaluationResult ruleResult,
        ScoreResult scoreResult,
        ScoreDelta scoreDelta,
        List<CertificationCandidate> addedCandidates,
        List<CertificationCandidate> removedCandidates,
        List<DocumentCode> newlyRequiredDocuments,
        List<DocumentCode> noLongerRequiredDocuments,
        List<DocumentCode> newlySatisfiedDocuments) {

    public SimulationOutcome {
        Guard.notNull(adjustedProfile, "adjustedProfile");
        Guard.notNull(ruleResult, "ruleResult");
        Guard.notNull(scoreResult, "scoreResult");
        Guard.notNull(scoreDelta, "scoreDelta");
        addedCandidates = List.copyOf(Guard.notNull(addedCandidates, "addedCandidates"));
        removedCandidates = List.copyOf(Guard.notNull(removedCandidates, "removedCandidates"));
        newlyRequiredDocuments =
                List.copyOf(Guard.notNull(newlyRequiredDocuments, "newlyRequiredDocuments"));
        noLongerRequiredDocuments =
                List.copyOf(Guard.notNull(noLongerRequiredDocuments, "noLongerRequiredDocuments"));
        newlySatisfiedDocuments =
                List.copyOf(Guard.notNull(newlySatisfiedDocuments, "newlySatisfiedDocuments"));
    }

    /**
     * 인증 후보 구성이 바뀌었는지. 바뀌었다면 단순히 점수만 오른 것이 아니라 <b>적용받는 제도 자체가
     * 달라진 것</b>이므로, 화면에서 훨씬 강하게 알려야 한다.
     */
    public boolean certificationScopeChanged() {
        return !addedCandidates.isEmpty() || !removedCandidates.isEmpty();
    }
}
