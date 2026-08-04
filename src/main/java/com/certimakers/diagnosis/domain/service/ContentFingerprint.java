package com.certimakers.diagnosis.domain.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * 원문 본문의 지문. 순수 함수다.
 *
 * <p>바이트를 그대로 해싱하지 않고 <b>공백을 정규화한 뒤</b> 해싱한다. 공식 기관 페이지는 배포
 * 때마다 들여쓰기·줄바꿈이 미세하게 달라지는데, 그때마다 "문서가 바뀌었다"고 알리면 경보가
 * 쌓여 아무도 보지 않게 된다. 우리가 알고 싶은 것은 <b>읽는 내용</b>이 달라졌는가다.
 */
public final class ContentFingerprint {

    private ContentFingerprint() {
    }

    /** 본문이 비어 있으면 지문도 없다 — 못 가져온 것과 빈 문서를 구별할 이유가 없다. */
    public static String of(String content) {
        if (content == null || content.isBlank()) {
            return null;
        }
        String normalized = content.replaceAll("\\s+", " ").trim();
        return sha256(normalized);
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            // SHA-256은 모든 JRE가 제공한다. 여기 오면 런타임이 규격을 어긴 것이다.
            throw new IllegalStateException("SHA-256을 사용할 수 없습니다.", e);
        }
    }
}
