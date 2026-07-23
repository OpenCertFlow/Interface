package com.certimakers.document.domain.model;

import static com.certimakers.document.domain.model.FieldType.DATE;
import static com.certimakers.document.domain.model.FieldType.MULTILINE;
import static com.certimakers.document.domain.model.FieldType.NUMBER;
import static com.certimakers.document.domain.model.FieldType.TEXT;

import java.util.List;

/**
 * 발급 가능한 문서 양식과 그 입력 항목.
 *
 * <p>{@code BoardType}과 같은 방식이다 — 양식 정의를 enum에 담아 새 양식 추가가 상수 하나로 끝나게
 * 한다. 양식을 DB 테이블로 두면 유연하지만, 항목이 바뀔 때마다 데이터 마이그레이션이 필요하고
 * 코드에서 항목 코드를 안전하게 참조할 수 없다. 해커톤 규모에서는 코드에 두는 편이 안전하다.
 *
 * <p><b>이 양식들은 서류를 대신 발급해 주지 않는다.</b> 사용자가 인증 기관에 제출할 서류를 작성할 때
 * 빠뜨리는 항목이 없도록 <b>초안</b>을 만들어 주는 것이 목적이며, 그 사실이 모든 산출물 하단에 고지된다.
 */
public enum DocumentTemplate {

    /** 공급자적합성확인 대상 제품의 자기적합성 선언 초안. */
    SELF_DECLARATION(
            "자기적합성 선언서(초안)",
            "공급자적합성확인 대상 제품에 대해 제조·수입자가 스스로 적합함을 선언하는 문서의 초안입니다.",
            List.of(
                    FormField.required("companyName", "업체명", TEXT, "예: 인증메이커스"),
                    FormField.required("businessNumber", "사업자등록번호", TEXT, "예: 123-45-67890"),
                    FormField.required("representative", "대표자명", TEXT, null),
                    FormField.required("productName", "제품명", TEXT, "예: 가정용 헤어드라이어"),
                    FormField.required("modelName", "모델명", TEXT, null),
                    FormField.required("ratedVoltage", "정격전압(V)", NUMBER, "예: 220"),
                    FormField.required("powerConsumption", "소비전력(W)", NUMBER, "예: 1200"),
                    FormField.required("declarationDate", "선언일", DATE, "YYYY-MM-DD"),
                    FormField.optional("remarks", "비고", MULTILINE, null))),

    /** 시험 의뢰·상담 시 제출하는 제품 사양 정리본. */
    PRODUCT_SPEC(
            "제품 사양서(초안)",
            "시험기관 의뢰나 컨설팅 상담 시 제품을 설명하기 위한 사양 정리본입니다.",
            List.of(
                    FormField.required("productName", "제품명", TEXT, null),
                    FormField.required("modelName", "모델명", TEXT, null),
                    FormField.required("productGroup", "제품군", TEXT, "예: 소형가전"),
                    FormField.required("ratedVoltage", "정격전압(V)", NUMBER, null),
                    FormField.required("powerConsumption", "소비전력(W)", NUMBER, null),
                    FormField.optional("hasBattery", "배터리 내장 여부", TEXT, "예: 없음 / 리튬이온"),
                    FormField.required("materials", "주요 재질", TEXT, "예: 플라스틱, 금속"),
                    FormField.required("targetUser", "사용 대상", TEXT, "예: 일반 / 어린이"),
                    FormField.required("salesChannel", "판매 방식", TEXT, "예: 온라인"),
                    FormField.optional("structure", "주요 구조·동작 설명", MULTILINE, null))),

    /** 제품 표시사항이 빠짐없이 들어갔는지 정리하는 계획서. */
    SAFETY_LABEL_PLAN(
            "안전표시 계획서(초안)",
            "제품·포장에 표시할 항목을 정리해 표시사항 누락을 사전에 점검하기 위한 문서입니다.",
            List.of(
                    FormField.required("productName", "제품명", TEXT, null),
                    FormField.required("modelName", "모델명", TEXT, null),
                    FormField.required("manufacturer", "제조자(수입자)", TEXT, null),
                    FormField.required("countryOfOrigin", "제조국", TEXT, "예: 대한민국"),
                    FormField.required("ratedVoltage", "정격전압 표시", TEXT, "예: AC 220V, 60Hz"),
                    FormField.required("powerConsumption", "소비전력 표시", TEXT, "예: 1200W"),
                    FormField.required("kcMarkPlan", "KC 마크 표시 위치", TEXT, "예: 제품 하단 라벨"),
                    FormField.optional("cautionNotes", "주의·경고 문구", MULTILINE, null))),

    /** 시험기관에 시험을 의뢰할 때 정리하는 요청 초안. */
    TEST_REQUEST(
            "시험 의뢰서(초안)",
            "시험기관에 시험을 의뢰하기 전에 필요한 정보를 정리하는 문서입니다.",
            List.of(
                    FormField.required("companyName", "업체명", TEXT, null),
                    FormField.required("contactName", "담당자명", TEXT, null),
                    FormField.required("contactPhone", "연락처", TEXT, null),
                    FormField.required("productName", "제품명", TEXT, null),
                    FormField.required("modelName", "모델명", TEXT, null),
                    FormField.required("requestedScheme", "의뢰 인증 제도", TEXT, "예: 안전확인"),
                    FormField.optional("sampleCount", "시료 수량", NUMBER, null),
                    FormField.optional("desiredDate", "희망 완료일", DATE, "YYYY-MM-DD"),
                    FormField.optional("remarks", "특이사항", MULTILINE, null)));

    private final String displayName;
    private final String description;
    private final List<FormField> fields;

    DocumentTemplate(String displayName, String description, List<FormField> fields) {
        this.displayName = displayName;
        this.description = description;
        this.fields = List.copyOf(fields);
    }

    public String displayName() {
        return displayName;
    }

    public String description() {
        return description;
    }

    public List<FormField> fields() {
        return fields;
    }

    public List<FormField> requiredFields() {
        return fields.stream().filter(FormField::required).toList();
    }

    public java.util.Optional<FormField> field(String code) {
        return fields.stream().filter(field -> field.code().equals(code)).findFirst();
    }
}
