package io.opencertflow.diagnosis.domain.model;

import io.opencertflow.common.domain.model.Guard;
import java.util.Optional;

/**
 * 발열 사양. 전기방석·전기요처럼 <b>열을 내며 신체에 닿는</b> 제품의 인증 판별 입력이다(F-APP-014~018).
 *
 * <p>전기 사양({@link ElectricalSpec})과 분리한 이유는 성격이 다르기 때문이다. 전압·소비전력은
 * 전기를 쓰는 모든 제품의 속성이지만, 표면온도·신체 접촉·과열 보호는 발열 제품에서만 의미가 있다.
 *
 * <p>발열 제품이 아니면 이 객체 자체를 갖지 않는다({@link ProductProfile}에서 null).
 *
 * <p><b>"모름"을 값으로 다룬다.</b> 온도조절기 유무({@link ControllerStatus#UNKNOWN})와 표면온도 출처
 * ({@link TemperatureSource#UNKNOWN})는 "없음/0"과 구분된다 — 모른다를 없음으로 뭉개면 잘못된 경고나
 * 판정이 나온다. 룰은 모름을 판단 불가(AMBIGUOUS_CONDITION)로 다룬다.
 *
 * @param bodyContactType            신체에 닿는 방식(F-APP-014). NONE이면 비접촉, UNKNOWN이면 모름
 * @param controllerStatus           온도조절기 유무(있음/없음/모름, F-APP-016)
 * @param adjustmentSteps            온도 조절 단계 수(예: 3단). 조절 방식이 STEP일 때만 값이 있다
 * @param maxSurfaceTemperatureCelsius 최고 표면온도(℃). 출처가 UNKNOWN이면 null
 * @param temperatureSource          표면온도 값의 출처(측정/추정/모름, F-APP-017)
 * @param medicalUseClaim            혈액순환·통증 완화 등 의료적 효능을 표방하는지(표방 시 의료기기 규제)
 * @param autoShutOff                일정 시간 뒤 자동으로 전원을 끄는 장치가 있는지
 * @param autoShutOffMinutes         자동으로 꺼지기까지의 시간(분). autoShutOff가 true일 때만 값이 있다
 * @param overheatProtection         과열 시 전원을 차단하는 장치가 있는지
 * @param removableCover             커버를 분리할 수 있는지(세탁을 위해)
 * @param washable                   물세탁이 가능한지
 * @param separableElectricParts     세탁 시 열선·컨트롤러 등 전기부를 분리할 수 있는지
 * @param hasSeparateAdapter         내장형이 아니라 별도 전원 어댑터를 쓰는지
 * @param adapterExternallyAttached  어댑터가 외장형인지(동봉/외장 구분). 어댑터가 없으면 null
 * @param adapterCertified           어댑터 자체가 KC 등 인증을 받았는지. 어댑터가 없으면 null
 * @param adjustmentMode             온도 조절 방식(단계/연속/기타, 결정문 §5.2). 조절기가 PRESENT일 때만
 *                                   값이 있다. 과열 차단과 별개로 표면온도 상한을 제한하는 장치가 있는지는
 *                                   {@code temperatureLimitDevice}로 따로 받는다
 * @param temperatureLimitDevice     표면온도 상한을 제한하는 장치가 있는지(과열 차단과 별개, 결정문 §5.2)
 */
public record HeatingSpec(
        BodyContactType bodyContactType,
        ControllerStatus controllerStatus,
        Integer adjustmentSteps,
        Integer maxSurfaceTemperatureCelsius,
        TemperatureSource temperatureSource,
        boolean medicalUseClaim,
        boolean autoShutOff,
        Integer autoShutOffMinutes,
        boolean overheatProtection,
        boolean removableCover,
        boolean washable,
        boolean separableElectricParts,
        boolean hasSeparateAdapter,
        Boolean adapterExternallyAttached,
        Boolean adapterCertified,
        AdjustmentMode adjustmentMode,
        boolean temperatureLimitDevice) {

    /** 사람이 접촉하는 제품에서 현실적으로 나올 수 있는 범위. 오타를 걸러 내는 것이 목적이다. */
    private static final int MIN_TEMPERATURE = 0;
    private static final int MAX_TEMPERATURE = 300;
    private static final int MAX_STEPS = 20;
    private static final int MAX_SHUTOFF_MINUTES = 1440; // 24시간

    public HeatingSpec {
        Guard.notNull(bodyContactType, "bodyContactType");
        Guard.notNull(controllerStatus, "controllerStatus");
        Guard.notNull(temperatureSource, "temperatureSource");

        // 조절 방식·단계는 조절기가 있을 때만 의미가 있다. 조절기가 있으면 방식(단계/연속/기타)이 필요하고,
        // 단계 수는 방식이 STEP일 때만 존재한다. "모름/없음"인데 방식·단계가 있으면 모순이다.
        if (controllerStatus == ControllerStatus.PRESENT) {
            Guard.notNull(adjustmentMode, "adjustmentMode");
            if (adjustmentMode == AdjustmentMode.STEP) {
                if (adjustmentSteps == null) {
                    throw new IllegalArgumentException("단계 조절이면 조절 단계 수를 입력해야 합니다.");
                }
                Guard.inRange(adjustmentSteps, 1, MAX_STEPS, "adjustmentSteps");
            } else if (adjustmentSteps != null) {
                throw new IllegalArgumentException("연속·기타 조절 방식이면 조절 단계 수는 비어 있어야 합니다.");
            }
        } else {
            if (adjustmentMode != null) {
                throw new IllegalArgumentException("온도조절기가 없거나 모름이면 조절 방식은 비어 있어야 합니다.");
            }
            if (adjustmentSteps != null) {
                throw new IllegalArgumentException("온도조절기가 없거나 모름이면 조절 단계는 비어 있어야 합니다.");
            }
        }

        // 표면온도와 출처는 짝이다. 출처가 모름이면 온도값이 없어야 하고, 측정·추정이면 온도값이 있어야 한다.
        if (temperatureSource == TemperatureSource.UNKNOWN) {
            if (maxSurfaceTemperatureCelsius != null) {
                throw new IllegalArgumentException("온도 출처가 '모름'이면 표면온도는 비어 있어야 합니다.");
            }
        } else {
            if (maxSurfaceTemperatureCelsius == null) {
                throw new IllegalArgumentException("온도 출처가 측정·추정이면 표면온도를 입력해야 합니다.");
            }
            Guard.inRange(
                    maxSurfaceTemperatureCelsius, MIN_TEMPERATURE, MAX_TEMPERATURE,
                    "maxSurfaceTemperatureCelsius");
        }

        // 자동 꺼짐 시간은 자동 차단이 있을 때만 의미가 있다.
        if (autoShutOff) {
            if (autoShutOffMinutes == null) {
                throw new IllegalArgumentException("자동 차단이 있으면 자동 꺼짐 시간(분)을 입력해야 합니다.");
            }
            Guard.inRange(autoShutOffMinutes, 1, MAX_SHUTOFF_MINUTES, "autoShutOffMinutes");
        } else if (autoShutOffMinutes != null) {
            throw new IllegalArgumentException("자동 차단이 없으면 자동 꺼짐 시간은 비어 있어야 합니다.");
        }

        // 어댑터가 없으면 세부 항목은 "해당 없음"(null)이어야 하고, 있으면 두 항목이 모두 있어야 한다.
        if (hasSeparateAdapter) {
            if (adapterExternallyAttached == null || adapterCertified == null) {
                throw new IllegalArgumentException(
                        "별도 어댑터를 쓰는 제품은 외장 여부와 어댑터 인증 여부를 모두 입력해야 합니다.");
            }
        } else if (adapterExternallyAttached != null || adapterCertified != null) {
            throw new IllegalArgumentException("어댑터가 없으면 어댑터 세부 항목은 비어 있어야 합니다.");
        }
    }

    public Optional<Integer> maxSurfaceTemperature() {
        return Optional.ofNullable(maxSurfaceTemperatureCelsius);
    }
}
