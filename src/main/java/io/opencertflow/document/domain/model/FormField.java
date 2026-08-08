package io.opencertflow.document.domain.model;

/**
 * 양식의 입력란 하나.
 *
 * @param code        값을 담을 키. 요청·저장 모두 이 코드를 쓴다
 * @param label       화면과 PDF에 표시되는 이름
 * @param type        입력 종류
 * @param required    필수 여부
 * @param placeholder 입력 예시. 없으면 null
 */
public record FormField(
        String code,
        String label,
        FieldType type,
        boolean required,
        String placeholder) {

    public static FormField required(String code, String label, FieldType type, String placeholder) {
        return new FormField(code, label, type, true, placeholder);
    }

    public static FormField optional(String code, String label, FieldType type, String placeholder) {
        return new FormField(code, label, type, false, placeholder);
    }
}
