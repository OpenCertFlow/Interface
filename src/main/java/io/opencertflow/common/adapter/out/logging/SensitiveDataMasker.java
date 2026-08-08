package io.opencertflow.common.adapter.out.logging;

import java.util.regex.Pattern;

/**
 * 로그 문자열의 개인정보를 마스킹한다(F-BE-015). 이메일·휴대폰·주민등록번호가 실수로 로그에 실려도
 * 원문이 남지 않게 한다 — 저장은 암호화가 막고, 로그는 이 마스킹이 막는다(두 겹).
 *
 * <p>정규식 기반이라 완벽하지는 않다. "형식에 맞는 값"만 가린다 — 목적은 사고성 노출을 줄이는 것이지
 * 모든 PII를 잡는 것이 아니다. 애초에 민감정보를 로그에 넣지 않는 것이 1차 방어다.
 */
public final class SensitiveDataMasker {

    // 주민등록번호: 생년월일 6자리 + 성별 1자리 + 6자리 → 뒷 7자리를 가린다.
    private static final Pattern RRN =
            Pattern.compile("\\b(\\d{6})[-\\s]?([1-4]\\d{6})\\b");

    // 휴대폰: 01x-xxxx-xxxx(구분자 유무 무관) → 가운데를 가리고 끝 4자리만 남긴다.
    private static final Pattern PHONE =
            Pattern.compile("\\b(01[0-9])[-\\s]?\\d{3,4}[-\\s]?(\\d{4})\\b");

    // 이메일: 로컬파트 앞 1자만 남기고 나머지를 가린다.
    private static final Pattern EMAIL =
            Pattern.compile("\\b([\\w.+-])[\\w.+-]*(@[\\w.-]+\\.[A-Za-z]{2,})");

    private SensitiveDataMasker() {
    }

    public static String mask(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        String masked = RRN.matcher(input).replaceAll("$1-*******");
        masked = PHONE.matcher(masked).replaceAll("$1-****-$2");
        masked = EMAIL.matcher(masked).replaceAll("$1***$2");
        return masked;
    }
}
