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

        /** 사용 중 신체에 직접 닿는지. 발열 제품이 아니면 null */
        Boolean directBodyContact,

        /** 온도조절기(과열 방지 장치) 유무. 발열 제품이 아니면 null */
        Boolean hasTemperatureController,

        /** 최고 표면온도(℃). 모르면 null — 룰이 판단 불가로 처리해 전문가 확인으로 보낸다 */
        @PositiveOrZero Integer maxSurfaceTemperatureCelsius) {

    public DiagnoseRequest {
        materials = materials != null ? materials : List.of();
        heldDocuments = heldDocuments != null ? heldDocuments : List.of();
    }

    /**
     * 발열 사양을 하나라도 보냈는지.
     *
     * <p>세 항목이 모두 없으면 발열 제품이 아닌 것으로 보고 {@code HeatingSpec}을 만들지 않는다.
     * 발열 사양이 없는 것과 "닿지 않는다"는 다른 상태이며, 후자로 뭉개면 발열 룰이 엉뚱한 제품에
     * 매칭될 수 있다.
     */
    public boolean hasHeatingInput() {
        return directBodyContact != null
                || hasTemperatureController != null
                || maxSurfaceTemperatureCelsius != null;
    }
}
