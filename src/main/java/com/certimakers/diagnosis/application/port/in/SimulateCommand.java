package com.certimakers.diagnosis.application.port.in;

import com.certimakers.common.domain.model.Guard;
import com.certimakers.diagnosis.domain.model.DiagnosisId;
import com.certimakers.diagnosis.domain.simulation.ProfileAdjustment;

/**
 * 시뮬레이션 요청. 기존 진단 하나와, 그 위에 얹을 가정을 가리킨다.
 *
 * @param diagnosisId 기준이 되는 원본 진단
 * @param adjustment  적용할 반사실 가정
 */
public record SimulateCommand(DiagnosisId diagnosisId, ProfileAdjustment adjustment) {

    public SimulateCommand {
        Guard.notNull(diagnosisId, "diagnosisId");
        Guard.notNull(adjustment, "adjustment");
    }
}
