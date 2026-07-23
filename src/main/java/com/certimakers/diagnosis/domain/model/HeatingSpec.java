package com.certimakers.diagnosis.domain.model;

import com.certimakers.common.domain.model.Guard;
import java.util.Optional;

/**
 * 발열 사양. 전기방석·전기요처럼 <b>열을 내며 신체에 닿는</b> 제품의 인증 판별 입력이다.
 *
 * <p>전기 사양({@link ElectricalSpec})과 분리한 이유는 성격이 다르기 때문이다. 전압·소비전력은
 * 전기를 쓰는 모든 제품의 속성이지만, 표면온도·신체 접촉은 발열 제품에서만 의미가 있다. 드라이기
 * 입력 화면에 표면온도를 묻는 칸이 뜨면 사용자는 무엇을 적어야 할지 모른다.
 *
 * <p>발열 제품이 아니면 이 객체 자체를 갖지 않는다({@link ProductProfile}에서 null).
 *
 * <p>{@code maxSurfaceTemperatureCelsius}가 null인 것은 <b>"모른다"</b>는 뜻이지 0도가 아니다.
 * 룰이 온도를 물었을 때 값이 없으면 매칭 실패가 아니라 <b>판단 불가</b>로 이어져야 하며,
 * 그 처리는 룰 작성자가 별도 룰(AMBIGUOUS_CONDITION)로 표현한다 — 전압과 같은 규약이다.
 *
 * @param directBodyContact          사용 중 신체에 직접 닿는지. 화상 위험 판단의 핵심 입력
 * @param hasTemperatureController   온도조절기(과열 방지 장치)를 갖췄는지
 * @param maxSurfaceTemperatureCelsius 최고 표면온도(℃). 측정하지 않았으면 null
 */
public record HeatingSpec(
        boolean directBodyContact,
        boolean hasTemperatureController,
        Integer maxSurfaceTemperatureCelsius) {

    /** 사람이 접촉하는 제품에서 현실적으로 나올 수 있는 범위. 오타를 걸러 내는 것이 목적이다. */
    private static final int MIN_TEMPERATURE = 0;
    private static final int MAX_TEMPERATURE = 300;

    public HeatingSpec {
        if (maxSurfaceTemperatureCelsius != null) {
            Guard.inRange(
                    maxSurfaceTemperatureCelsius,
                    MIN_TEMPERATURE, MAX_TEMPERATURE,
                    "maxSurfaceTemperatureCelsius");
        }
    }

    public Optional<Integer> maxSurfaceTemperature() {
        return Optional.ofNullable(maxSurfaceTemperatureCelsius);
    }
}
