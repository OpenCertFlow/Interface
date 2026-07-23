package com.certimakers.diagnosis.adapter.in.web;

import com.certimakers.diagnosis.domain.model.InputField;
import com.certimakers.diagnosis.domain.model.ProductGroup;
import java.util.List;

/**
 * 제품군 메타데이터 응답. 앱이 <b>입력 화면을 서버 정의대로 그리기 위한</b> 정보다.
 *
 * <p>제품군마다 묻는 항목이 다르다(전기방석은 표면온도를 묻고 드라이기는 묻지 않는다). 그 차이를
 * 앱에 하드코딩하면 제품군을 추가할 때마다 앱을 고쳐야 하고, 서버 룰이 기대하는 입력과 화면이
 * 어긋날 수 있다.
 */
public record ProductGroupResponse(
        String code,
        String displayName,
        String description,
        List<FieldView> fields) {

    public static List<ProductGroupResponse> all() {
        return java.util.Arrays.stream(ProductGroup.values())
                .map(ProductGroupResponse::from)
                .toList();
    }

    public static ProductGroupResponse from(ProductGroup group) {
        return new ProductGroupResponse(
                group.name(),
                group.displayName(),
                group.description(),
                group.inputFields().stream().map(FieldView::from).toList());
    }

    /**
     * 입력 항목 하나.
     *
     * @param dependsOn 이 불리언 항목이 참일 때만 노출·필수가 된다. 없으면 항상 노출
     *                  (예: {@code ratedVoltage}는 {@code usesElectricity}에 의존)
     * @param options   SINGLE_SELECT·MULTI_SELECT일 때의 보기. 그 외에는 빈 목록
     */
    public record FieldView(
            String code,
            String label,
            String type,
            boolean required,
            String dependsOn,
            String helpText,
            List<OptionView> options) {

        static FieldView from(InputField field) {
            return new FieldView(
                    field.code(),
                    field.label(),
                    field.type().name(),
                    field.required(),
                    field.dependsOn(),
                    field.helpText(),
                    field.options().stream()
                            .map(option -> new OptionView(option.code(), option.label()))
                            .toList());
        }
    }

    public record OptionView(String code, String label) {
    }
}
