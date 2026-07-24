package com.certimakers.diagnosis.domain.model;

import com.certimakers.common.domain.model.Guard;
import java.util.Optional;

/**
 * 발열 사양. 전기방석·전기요처럼 <b>열을 내며 신체에 닿는</b> 제품의 인증 판별 입력이다.
 *
 * <p>전기 사양({@link ElectricalSpec})과 분리한 이유는 성격이 다르기 때문이다. 전압·소비전력은
 * 전기를 쓰는 모든 제품의 속성이지만, 표면온도·신체 접촉·과열 보호는 발열 제품에서만 의미가 있다.
 * 드라이기 입력 화면에 표면온도를 묻는 칸이 뜨면 사용자는 무엇을 적어야 할지 모른다.
 *
 * <p>발열 제품이 아니면 이 객체 자체를 갖지 않는다({@link ProductProfile}에서 null).
 *
 * <p>항목은 기능정의서(F-APP-014~018, 전기방석 상세 입력)를 따른다:
 * <ul>
 *   <li>APP-EC-01 일반 보온용 / 의료적 표현 여부 → {@code medicalUseClaim}
 *   <li>APP-EC-02 전원·어댑터 → {@code hasSeparateAdapter}·{@code adapterExternallyAttached}·{@code adapterCertified}
 *   <li>APP-EC-03 열선·온도조절기 → {@code hasTemperatureController}
 *   <li>APP-EC-04 자동 차단·과열 보호 → {@code autoShutOff}·{@code overheatProtection}
 *   <li>APP-EC-05 커버·세탁·전기부 분리 → {@code removableCover}·{@code washable}·{@code separableElectricParts}
 * </ul>
 *
 * <p>{@code maxSurfaceTemperatureCelsius}가 null인 것은 <b>"모른다"</b>는 뜻이지 0도가 아니다.
 * 룰이 온도를 물었을 때 값이 없으면 매칭 실패가 아니라 <b>판단 불가</b>로 이어져야 하며,
 * 그 처리는 룰 작성자가 별도 룰(AMBIGUOUS_CONDITION)로 표현한다 — 전압과 같은 규약이다.
 *
 * <p>어댑터 관련 두 항목({@code adapterExternallyAttached}·{@code adapterCertified})은 별도 어댑터가
 * 없으면 null이다. 이 역시 "모른다/해당 없음"과 "false"를 구분하기 위함이다.
 *
 * @param directBodyContact          사용 중 신체에 직접 닿는지. 화상 위험 판단의 핵심 입력
 * @param hasTemperatureController   온도조절기(온도 단계 조절 장치)를 갖췄는지
 * @param maxSurfaceTemperatureCelsius 최고 표면온도(℃). 측정하지 않았으면 null
 * @param medicalUseClaim            혈액순환·통증 완화 등 의료적 효능을 표방하는지. 표방하면 의료기기
 *                                   규제 영역으로 넘어가 전문가 확인이 필요하다
 * @param autoShutOff                일정 시간 뒤 자동으로 전원을 끄는 장치가 있는지
 * @param overheatProtection         과열 시 전원을 차단하는 온도 제한 장치가 있는지
 * @param removableCover             커버를 분리할 수 있는지(세탁을 위해)
 * @param washable                   물세탁이 가능한지
 * @param separableElectricParts     세탁 시 열선·컨트롤러 등 전기부를 분리할 수 있는지
 * @param hasSeparateAdapter         내장형이 아니라 별도 전원 어댑터를 쓰는지
 * @param adapterExternallyAttached  어댑터가 제품과 분리된 외장형인지(동봉/외장 구분). 어댑터가 없으면 null
 * @param adapterCertified           어댑터 자체가 KC 등 인증을 받았는지. 어댑터가 없으면 null
 */
public record HeatingSpec(
        boolean directBodyContact,
        boolean hasTemperatureController,
        Integer maxSurfaceTemperatureCelsius,
        boolean medicalUseClaim,
        boolean autoShutOff,
        boolean overheatProtection,
        boolean removableCover,
        boolean washable,
        boolean separableElectricParts,
        boolean hasSeparateAdapter,
        Boolean adapterExternallyAttached,
        Boolean adapterCertified) {

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
        // 어댑터가 없으면 어댑터 세부 항목은 "해당 없음"(null)이어야 한다. 어댑터가 있으면 두 항목이
        // 모두 있어야 한다 — "모른다"를 false로 뭉개면 인증 어댑터로 인한 범위 판단이 조용히 뒤집힌다.
        if (hasSeparateAdapter) {
            if (adapterExternallyAttached == null || adapterCertified == null) {
                throw new IllegalArgumentException(
                        "별도 어댑터를 쓰는 제품은 외장 여부와 어댑터 인증 여부를 모두 입력해야 합니다.");
            }
        } else {
            if (adapterExternallyAttached != null || adapterCertified != null) {
                throw new IllegalArgumentException(
                        "어댑터가 없으면 어댑터 세부 항목은 비어 있어야 합니다.");
            }
        }
    }

    public Optional<Integer> maxSurfaceTemperature() {
        return Optional.ofNullable(maxSurfaceTemperatureCelsius);
    }
}
