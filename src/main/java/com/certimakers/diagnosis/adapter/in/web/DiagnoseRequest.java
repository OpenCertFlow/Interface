package com.certimakers.diagnosis.adapter.in.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.List;

/**
 * 진단 요청 본문. 웹 계층의 DTO이며 원시 타입만 담는다. enum 변환과 도메인 조립은
 * {@link DiagnosisWebMapper}가 한다.
 *
 * <p>Bean Validation은 형식만 검사한다("전압은 0 이상"). "전기를 안 쓰면 전압이 없어야 한다" 같은
 * 도메인 규칙은 {@code ElectricalSpec} 생성자가 강제한다 — 검증 책임이 계층별로 나뉜다.
 */
public record DiagnoseRequest(
        @NotBlank String productName,
        @NotBlank String productGroup,
        @NotNull Boolean usesElectricity,
        @PositiveOrZero Integer ratedVoltage,
        @PositiveOrZero Integer powerConsumption,
        @NotNull Boolean hasBattery,
        @NotBlank String targetUser,
        @NotBlank String salesChannel,
        List<String> materials,
        List<String> heldDocuments,

        // ── 발열 제품(전기방석 등) 전용. 소형가전에서는 보내지 않는다 ──
        // 어떤 제품군이 어떤 항목을 요구하는지는 GET /api/v1/product-groups가 알려 준다.

        /** 신체에 닿는 방식(BodyContactType). 발열 제품이 아니면 null */
        String bodyContactType,

        /** 온도조절기 유무(ControllerStatus: PRESENT/ABSENT/UNKNOWN). 발열 제품이 아니면 null */
        String controllerStatus,

        /** 온도 조절 단계 수. 조절 방식이 STEP일 때만 값이 있다 */
        @PositiveOrZero Integer adjustmentSteps,

        /** 온도 조절 방식(AdjustmentMode: STEP/CONTINUOUS/OTHER). 조절기가 있을 때만 값이 있다 */
        String adjustmentMode,

        /** 최고 표면온도(℃). 출처가 모름이면 null */
        @PositiveOrZero Integer maxSurfaceTemperatureCelsius,

        /** 표면온도 값의 출처(TemperatureSource: MEASURED/ESTIMATED/UNKNOWN) */
        String temperatureSource,

        /** 혈액순환·통증 완화 등 의료적 효능을 표방하는지. 표방하면 의료기기 규제로 넘어간다 */
        Boolean medicalUseClaim,

        /** 일정 시간 뒤 자동 전원 차단 장치가 있는지 */
        Boolean autoShutOff,

        /** 자동으로 꺼지기까지의 시간(분). 자동 차단이 있을 때만 값이 있다 */
        @PositiveOrZero Integer autoShutOffMinutes,

        /** 과열 시 전원을 차단하는 장치가 있는지 */
        Boolean overheatProtection,

        /** 표면온도 상한을 제한하는 장치가 있는지(과열 차단과 별개) */
        Boolean temperatureLimitDevice,

        /** 커버를 분리할 수 있는지(세탁을 위해) */
        Boolean removableCover,

        /** 물세탁이 가능한지 */
        Boolean washable,

        /** 세탁 시 열선·컨트롤러 등 전기부를 분리할 수 있는지 */
        Boolean separableElectricParts,

        /** 내장형이 아니라 별도 전원 어댑터를 쓰는지 */
        Boolean hasSeparateAdapter,

        /** 어댑터가 외장형인지(동봉/외장 구분). 어댑터가 없으면 null */
        Boolean adapterExternallyAttached,

        /** 어댑터 자체가 KC 등 인증을 받았는지. 어댑터가 없으면 null */
        Boolean adapterCertified) {

    public DiagnoseRequest {
        materials = materials != null ? materials : List.of();
        heldDocuments = heldDocuments != null ? heldDocuments : List.of();
    }

    /**
     * 발열 사양을 하나라도 보냈는지.
     *
     * <p>발열 관련 항목이 모두 없으면 발열 제품이 아닌 것으로 보고 {@code HeatingSpec}을 만들지 않는다.
     * 발열 사양이 없는 것과 "닿지 않는다"는 다른 상태이며, 후자로 뭉개면 발열 룰이 엉뚱한 제품에
     * 매칭될 수 있다. 신체 접촉·온도조절기 유무만으로 판단한다 — 나머지 세부 항목은 이 둘이 있을 때만
     * 의미가 있고, {@link DiagnosisWebMapper}가 함께 요구한다.
     */
    public boolean hasHeatingInput() {
        return bodyContactType != null
                || controllerStatus != null
                || temperatureSource != null
                || maxSurfaceTemperatureCelsius != null
                || medicalUseClaim != null
                || autoShutOff != null
                || overheatProtection != null
                || removableCover != null
                || washable != null
                || separableElectricParts != null
                || hasSeparateAdapter != null;
    }
}
