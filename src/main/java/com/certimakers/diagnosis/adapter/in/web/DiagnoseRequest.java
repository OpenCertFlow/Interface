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
        List<String> heldDocuments) {

    public DiagnoseRequest {
        materials = materials != null ? materials : List.of();
        heldDocuments = heldDocuments != null ? heldDocuments : List.of();
    }
}
