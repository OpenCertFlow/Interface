package com.certimakers.diagnosis.domain.model;

import com.certimakers.common.domain.model.Guard;
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
 */
public record ElectricalSpec(
        boolean usesElectricity,
        Integer ratedVoltage,
        Integer powerConsumption,
        boolean hasBattery) {

    public ElectricalSpec {
        if (!usesElectricity && (ratedVoltage != null || powerConsumption != null)) {
            throw com.certimakers.common.domain.error.BusinessException.invalid(
                    "전기를 사용하지 않는 제품에 정격전압·소비전력이 입력되었습니다.");
        }
        if (ratedVoltage != null) {
            Guard.inRange(ratedVoltage, 0, 100_000, "ratedVoltage");
        }
        if (powerConsumption != null) {
            Guard.inRange(powerConsumption, 0, 1_000_000, "powerConsumption");
        }
    }

    public static ElectricalSpec nonElectric() {
        return new ElectricalSpec(false, null, null, false);
    }

    public Optional<Integer> ratedVoltageValue() {
        return Optional.ofNullable(ratedVoltage);
    }

    public Optional<Integer> powerConsumptionValue() {
        return Optional.ofNullable(powerConsumption);
    }
}
