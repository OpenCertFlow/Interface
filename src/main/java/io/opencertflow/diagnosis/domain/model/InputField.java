package io.opencertflow.diagnosis.domain.model;

import java.util.List;

/**
 * 진단 입력 화면의 항목 하나.
 *
 * <p>제품군마다 물어야 할 것이 다르다 — 전기방석에는 표면온도를 묻지만 드라이기에는 묻지 않는다.
 * 그 차이를 앱에 하드코딩하면 제품군을 추가할 때마다 앱을 고쳐야 하고, 서버 룰이 기대하는 입력과
 * 화면이 어긋날 수 있다. 그래서 <b>서버가 입력 스키마를 내려보내고 앱은 그대로 그린다.</b>
 *
 * @param code      값을 담을 키. 진단 요청 본문의 필드명과 일치한다
 * @param label     화면에 표시되는 이름
 * @param type      입력 위젯 종류
 * @param required  필수 여부. {@code dependsOn}이 있으면 그 조건이 참일 때만 필수다
 * @param dependsOn 이 불리언 항목이 참일 때만 노출한다. 없으면 항상 노출
 * @param helpText  입력 도움말. 없으면 null
 * @param options   선택 항목. SINGLE_SELECT·MULTI_SELECT일 때만 채워진다
 */
public record InputField(
        String code,
        String label,
        InputFieldType type,
        boolean required,
        String dependsOn,
        String helpText,
        List<InputOption> options) {

    public InputField {
        options = options == null ? List.of() : List.copyOf(options);
    }

    public static InputField text(String code, String label, boolean required, String helpText) {
        return new InputField(code, label, InputFieldType.TEXT, required, null, helpText, List.of());
    }

    public static InputField bool(String code, String label, String helpText) {
        return new InputField(code, label, InputFieldType.BOOLEAN, true, null, helpText, List.of());
    }

    /** 조건부 숫자 입력. {@code dependsOn} 항목이 참일 때만 노출·필수가 된다. */
    public static InputField integerWhen(
            String code, String label, boolean required, String dependsOn, String helpText) {
        return new InputField(
                code, label, InputFieldType.INTEGER, required, dependsOn, helpText, List.of());
    }

    public static InputField singleSelect(
            String code, String label, List<InputOption> options, String helpText) {
        return new InputField(
                code, label, InputFieldType.SINGLE_SELECT, true, null, helpText, options);
    }

    public static InputField multiSelect(
            String code, String label, List<InputOption> options, boolean required, String helpText) {
        return new InputField(
                code, label, InputFieldType.MULTI_SELECT, required, null, helpText, options);
    }

    /** 조건부 불리언 입력. */
    public static InputField boolWhen(String code, String label, String dependsOn, String helpText) {
        return new InputField(
                code, label, InputFieldType.BOOLEAN, true, dependsOn, helpText, List.of());
    }
}
