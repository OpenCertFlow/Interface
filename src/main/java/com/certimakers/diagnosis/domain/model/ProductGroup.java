package com.certimakers.diagnosis.domain.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 제품군과 <b>제품군별 입력 스키마</b>. 룰셋은 제품군별로 버전된다.
 *
 * <p>제품군마다 물어야 할 것이 다르다. 전기방석에는 표면온도·신체 접촉을 묻지만 드라이기에는 묻지
 * 않는다. 그 차이를 앱에 하드코딩하면 제품군을 추가할 때마다 앱을 고쳐야 하고, 서버 룰이 기대하는
 * 입력과 화면이 어긋날 수 있다. 그래서 입력 스키마를 여기 두고 API로 내려보낸다
 * ({@code GET /api/v1/product-groups}).
 *
 * <p>{@code BoardType}·{@code DocumentTemplate}과 같은 방식이다 — 제품군 추가가 상수 하나로 끝난다.
 */
public enum ProductGroup {

    /**
     * 소형가전. 대표 시나리오는 가정용 헤어드라이어다(기획서).
     *
     * <p>기본 진단 흐름의 안정성과 기획서 연속성을 확보하는 기준 제품군이다.
     */
    SMALL_APPLIANCE(
            "소형가전",
            "드라이어·전기포트 등 일반 소형 전기용품",
            commonFields()),

    /**
     * 일반 보온용 전기방석.
     *
     * <p>기존 생활소품(방석·쿠션)에 열선·온도조절기를 결합해 판매하려는 봉제 소공인의 상황을
     * 다룬다. 신체에 직접 닿고 장시간 사용하는 발열 제품이라 소형가전과 조건 분기가 달라진다.
     */
    ELECTRIC_HEATING_PAD(
            "일반 보온용 전기방석",
            "방석·쿠션에 열선과 온도조절기를 결합한 보온 제품",
            heatingFields());

    private final String displayName;
    private final String description;
    private final List<InputField> inputFields;

    ProductGroup(String displayName, String description, List<InputField> inputFields) {
        this.displayName = displayName;
        this.description = description;
        this.inputFields = List.copyOf(inputFields);
    }

    public String displayName() {
        return displayName;
    }

    public String description() {
        return description;
    }

    public List<InputField> inputFields() {
        return inputFields;
    }

    /** 발열 사양을 입력받는 제품군인지. 앱은 이 값을 볼 필요 없이 스키마를 그대로 따르면 된다. */
    public boolean requiresHeatingSpec() {
        return inputFields.stream().anyMatch(field -> field.code().equals("directBodyContact"));
    }

    // ── 입력 스키마 정의 ──────────────────────────────────────────

    /** 모든 제품군이 공통으로 묻는 항목. */
    private static List<InputField> commonFields() {
        return List.of(
                InputField.text("productName", "제품명", true, "예: 가정용 헤어드라이어"),
                InputField.bool("usesElectricity", "전기를 사용하나요?", null),
                InputField.integerWhen("ratedVoltage", "정격전압(V)", true,
                        "usesElectricity", "제품에 표시된 값. 예: 220"),
                InputField.integerWhen("powerConsumption", "소비전력(W)", true,
                        "usesElectricity", "제품에 표시된 값. 예: 1200"),
                InputField.bool("hasBattery", "배터리가 내장되어 있나요?", null),
                InputField.singleSelect("targetUser", "주 사용 대상", targetUserOptions(),
                        "어린이용은 별도 인증이 필요할 수 있습니다"),
                InputField.singleSelect("salesChannel", "판매 방식", salesChannelOptions(), null),
                InputField.multiSelect("materials", "주요 재질", materialOptions(), true, null),
                InputField.multiSelect("heldDocuments", "이미 보유한 서류", documentOptions(), false,
                        "가지고 있는 것만 체크해 주세요. 준비도 점수 계산에 쓰입니다"));
    }

    /**
     * 공통 항목 + 발열 제품 전용 항목.
     *
     * <p>표면온도를 <b>선택 입력</b>으로 둔 이유는 소공인이 측정값을 모르는 경우가 흔하기 때문이다.
     * 값이 없으면 룰이 판단 불가로 처리해 전문가 확인으로 보낸다 — 모른다고 진단 자체를 막지 않는다.
     */
    private static List<InputField> heatingFields() {
        List<InputField> fields = new ArrayList<>(commonFields());
        fields.add(InputField.bool("directBodyContact",
                "사용 중 신체에 직접 닿나요?",
                "방석·요처럼 몸에 닿는 제품은 화상 위험 기준이 달라집니다"));
        fields.add(InputField.bool("hasTemperatureController",
                "온도조절기(과열 방지 장치)가 있나요?",
                "온도조절기 유무에 따라 요구되는 시험 항목이 달라질 수 있습니다"));
        fields.add(new InputField(
                "maxSurfaceTemperatureCelsius", "최고 표면온도(℃)",
                InputFieldType.INTEGER, false, null,
                "측정값을 모르면 비워 두세요. 전문가 확인 항목으로 안내됩니다", List.of()));
        return fields;
    }

    private static List<InputOption> targetUserOptions() {
        return Arrays.stream(TargetUser.values())
                .map(value -> new InputOption(value.name(), value.displayName()))
                .toList();
    }

    private static List<InputOption> salesChannelOptions() {
        return Arrays.stream(SalesChannel.values())
                .map(value -> new InputOption(value.name(), value.displayName()))
                .toList();
    }

    private static List<InputOption> materialOptions() {
        return Arrays.stream(MaterialType.values())
                .map(value -> new InputOption(value.name(), value.displayName()))
                .toList();
    }

    /**
     * 보유 서류 체크 목록.
     *
     * <p>실제로 <b>요구되는</b> 서류는 룰 평가 결과로 정해지지만, 그것을 알려면 먼저 입력을 받아야
     * 해서 순환이 된다. 그래서 입력 단계에서는 흔히 쓰이는 서류를 보기로 제시하고, 진단 후 리포트가
     * 실제 요구 서류를 알려 준다.
     */
    private static List<InputOption> documentOptions() {
        return List.of(
                new InputOption("BIZ_LICENSE", "사업자등록증"),
                new InputOption("TEST_REPORT", "시험성적서"),
                new InputOption("CIRCUIT_DIAGRAM", "회로도"),
                new InputOption("PARTS_LIST", "부품표"),
                new InputOption("SAFETY_LABEL_SAMPLE", "안전표시 라벨 견본"));
    }
}
