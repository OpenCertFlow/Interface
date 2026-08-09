package io.opencertflow.diagnosis.domain.model;

import io.opencertflow.common.domain.model.Guard;
import java.util.Optional;

/**
 * 전기적 사양. 소형가전 인증 판별의 핵심 입력이다.
 *
 * <p>{@code ratedVoltage}와 {@code powerConsumption}은 전기를 쓰지 않으면 의미가 없으므로
 * nullable이다. 룰이 "정격전압 &gt; 50V"를 물을 때 값이 없으면, 조건은 매칭 실패가 아니라
 * <b>판단 불가</b>로 이어져야 한다 — 그 처리는 룰 작성자가 별도 룰(AMBIGUOUS_CONDITION)로 표현한다.
 *
 * @param usesElectricity   전기 사용 여부
 * @param ratedVoltage      정격전압(V). 전기 미사용 시 null
 * @param powerConsumption  소비전력(W). 전기 미사용 시 null
 * @param hasBattery        배터리 내장 여부
 * @param powerSource       전원 방식(교류/직류/모름). <b>인증 등급을 가르는 값이다</b> —
 *                          시행규칙이 같은 품목도 전원 방식에 따라 다른 별표에 넣는다.
 *                          정격전압이나 배터리 유무로 추측하지 않는다({@link PowerSource} 참조)
 */
public record ElectricalSpec(
        boolean usesElectricity,
        Integer ratedVoltage,
        Integer powerConsumption,
        boolean hasBattery,
        PowerSource powerSource) {

    public ElectricalSpec {
        if (!usesElectricity && (ratedVoltage != null || powerConsumption != null)) {
            throw io.opencertflow.common.domain.error.BusinessException.invalid(
                    "전기를 사용하지 않는 제품에 정격전압·소비전력이 입력되었습니다.");
        }
        // 입력하지 않았으면 '모름'이다. null로 두면 룰이 이 값을 물을 때마다 분기가 늘어나고,
        // 결국 어딘가에서 '모름'과 '없음'이 뭉개진다.
        if (powerSource == null) {
            powerSource = PowerSource.UNKNOWN;
        }
        if (ratedVoltage != null) {
            Guard.inRange(ratedVoltage, 0, 100_000, "ratedVoltage");
        }
        if (powerConsumption != null) {
            Guard.inRange(powerConsumption, 0, 1_000_000, "powerConsumption");
        }
    }

    public static ElectricalSpec nonElectric() {
        return new ElectricalSpec(false, null, null, false, PowerSource.UNKNOWN);
    }

    public Optional<Integer> ratedVoltageValue() {
        return Optional.ofNullable(ratedVoltage);
    }

    public Optional<Integer> powerConsumptionValue() {
        return Optional.ofNullable(powerConsumption);
    }
}
