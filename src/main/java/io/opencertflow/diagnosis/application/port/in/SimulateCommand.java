package io.opencertflow.diagnosis.application.port.in;

import io.opencertflow.common.domain.model.Guard;
import io.opencertflow.diagnosis.domain.model.DiagnosisId;
import io.opencertflow.diagnosis.domain.simulation.ProfileAdjustment;

/**
 * 시뮬레이션 요청. 기존 진단 하나와, 그 위에 얹을 가정을 가리킨다.
 *
 * @param diagnosisId 기준이 되는 원본 진단
 * @param adjustment  적용할 반사실 가정
 */
/**
 * @param viewerUserId 요청자. {@code null}이면 비로그인이다. 소유자가 있는 진단은 본인만
 *                     시뮬레이션할 수 있다 — 남의 진단으로 가정을 바꿔 보는 것도 열람이다.
 */
public record SimulateCommand(
        DiagnosisId diagnosisId, ProfileAdjustment adjustment, String viewerUserId) {

    public SimulateCommand {
        Guard.notNull(diagnosisId, "diagnosisId");
        Guard.notNull(adjustment, "adjustment");
    }
}
