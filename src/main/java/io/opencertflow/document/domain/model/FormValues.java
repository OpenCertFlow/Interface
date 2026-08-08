package io.opencertflow.document.domain.model;

import io.opencertflow.common.domain.error.BusinessException;
import io.opencertflow.document.domain.error.DocumentErrorCode;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 양식에 채워 넣은 값. <b>양식 정의에 비추어 스스로를 검증한다.</b>
 *
 * <p>검증을 값 객체에 두는 이유는, 검증되지 않은 값이 담긴 {@code FormValues}가 아예 존재할 수 없게
 * 하기 위함이다. 서비스가 검증을 잊어도 이 타입을 만드는 순간 규칙이 적용된다.
 *
 * <p>양식에 없는 항목을 조용히 버리지 않고 <b>거부</b>한다. 오타 난 항목 코드를 무시하면 사용자는
 * 입력이 반영된 줄 알지만 문서에는 빠진 채로 나온다.
 */
public record FormValues(DocumentTemplate template, Map<String, String> values) {

    public FormValues {
        Map<String, String> normalized = normalize(template, values);
        values = Map.copyOf(normalized);
    }

    private static Map<String, String> normalize(DocumentTemplate template, Map<String, String> raw) {
        Map<String, String> cleaned = new LinkedHashMap<>();

        for (Map.Entry<String, String> entry : raw.entrySet()) {
            FormField field = template.field(entry.getKey())
                    .orElseThrow(() -> new BusinessException(
                            DocumentErrorCode.UNKNOWN_FIELD,
                            "'%s' 항목은 %s 양식에 없습니다.".formatted(entry.getKey(), template.displayName()),
                            Map.of("field", entry.getKey())));

            String value = entry.getValue() == null ? "" : entry.getValue().strip();
            if (!value.isEmpty()) {
                validate(field, value);
                cleaned.put(field.code(), value);
            }
        }

        List<String> missing = template.requiredFields().stream()
                .map(FormField::code)
                .filter(code -> !cleaned.containsKey(code))
                .toList();
        if (!missing.isEmpty()) {
            throw new BusinessException(
                    DocumentErrorCode.REQUIRED_FIELD_MISSING,
                    "필수 항목이 비어 있습니다: %s".formatted(labelsOf(template, missing)),
                    Map.of("missingFields", missing));
        }
        return cleaned;
    }

    private static void validate(FormField field, String value) {
        if (value.length() > field.type().maxLength()) {
            throw new BusinessException(
                    DocumentErrorCode.INVALID_FIELD_VALUE,
                    "'%s' 항목은 %d자 이하로 입력해 주세요.".formatted(field.label(), field.type().maxLength()),
                    Map.of("field", field.code()));
        }
        switch (field.type()) {
            case DATE -> requireDate(field, value);
            case NUMBER -> requireNumber(field, value);
            case TEXT, MULTILINE -> {
                // 길이 검증으로 충분하다
            }
        }
    }

    private static void requireDate(FormField field, String value) {
        try {
            LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw new BusinessException(
                    DocumentErrorCode.INVALID_FIELD_VALUE,
                    "'%s' 항목은 YYYY-MM-DD 형식으로 입력해 주세요.".formatted(field.label()),
                    Map.of("field", field.code()));
        }
    }

    private static void requireNumber(FormField field, String value) {
        if (!value.matches("^\\d+(\\.\\d+)?$")) {
            throw new BusinessException(
                    DocumentErrorCode.INVALID_FIELD_VALUE,
                    "'%s' 항목은 숫자로 입력해 주세요.".formatted(field.label()),
                    Map.of("field", field.code()));
        }
    }

    private static String labelsOf(DocumentTemplate template, List<String> codes) {
        return codes.stream()
                .map(code -> template.field(code).map(FormField::label).orElse(code))
                .collect(java.util.stream.Collectors.joining(", "));
    }

    public Optional<String> valueOf(String fieldCode) {
        return Optional.ofNullable(values.get(fieldCode));
    }

    /** 양식에 정의된 순서대로 (라벨, 값) 쌍을 돌려준다. PDF와 화면이 같은 순서를 쓰게 한다. */
    public List<Map.Entry<String, String>> orderedLabelledValues() {
        return template.fields().stream()
                .filter(field -> values.containsKey(field.code()))
                .map(field -> Map.entry(field.label(), values.get(field.code())))
                .toList();
    }
}
