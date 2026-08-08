package io.opencertflow.file.domain.model;

import java.util.Locale;

/**
 * 파일의 MIME 타입.
 *
 * <p>클라이언트가 보낸 값은 신뢰할 수 없다 — 확장자만 바꿔도 바뀐다. 그래서 이 값은 <b>다운로드 시
 * 응답 헤더에 쓰는 힌트</b>로만 취급하고, 접근 제어의 근거로 삼지 않는다.
 *
 * <p>비어 있으면 {@code application/octet-stream}으로 둔다. 브라우저가 무엇인지 모를 때 실행하지 않고
 * 내려받게 하는 가장 안전한 기본값이다.
 */
public record ContentType(String value) {

    private static final String DEFAULT = "application/octet-stream";

    public ContentType {
        if (value == null || value.isBlank()) {
            value = DEFAULT;
        } else {
            value = value.strip().toLowerCase(Locale.ROOT);
        }
    }

    public static ContentType of(String value) {
        return new ContentType(value);
    }

    public static ContentType octetStream() {
        return new ContentType(DEFAULT);
    }

    /**
     * 브라우저가 인라인으로 렌더링해도 안전한 형식인지.
     *
     * <p>HTML·SVG를 인라인으로 열어 주면 저장된 스크립트가 우리 도메인에서 실행된다(저장형 XSS).
     * 이미지·PDF만 인라인을 허용하고 나머지는 첨부로 내려보낸다.
     */
    public boolean safeToRenderInline() {
        return value.startsWith("image/") && !value.contains("svg")
                || value.equals("application/pdf");
    }
}
