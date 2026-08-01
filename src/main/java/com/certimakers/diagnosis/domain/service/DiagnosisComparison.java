package com.certimakers.diagnosis.domain.service;

import com.certimakers.diagnosis.domain.model.DiagnosisId;
import com.certimakers.diagnosis.domain.model.DocumentCode;
import com.certimakers.diagnosis.domain.simulation.ScoreDelta;
import java.util.List;

/**
 * 원 진단과 그 재진단의 비교 결과(F-APP-048). 계산은 {@link DiagnosisComparator}가 한다.
 *
 * <p>DB에 저장하지 않는 계산 산출물이라 {@code model/}이 아닌 여기에 둔다({@code ScoreResult}와 같은 결).
 *
 * @param previousId      원 진단 ID
 * @param currentId       재진단 ID
 * @param delta           준비도 점수 변화 (시뮬레이션의 ScoreDelta 재활용)
 * @param newlyHeld       원본에선 없었는데 재진단에서 갖춘 서류
 * @param stillMissing    재진단 시점에도 여전히 없는 서류
 * @param baselineDiffers 두 진단의 룰셋 버전이 다른지 (다르면 점수 차를 개선으로 단정할 수 없다)
 */
public record DiagnosisComparison(
        DiagnosisId previousId,
        DiagnosisId currentId,
        ScoreDelta delta,
        List<DocumentCode> newlyHeld,
        List<DocumentCode> stillMissing,
        boolean baselineDiffers) {
}