package io.opencertflow.diagnosis.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 원문 지문의 성질을 고정한다.
 *
 * <p>핵심은 <b>거짓 변경을 만들지 않는 것</b>이다. 페이지 배포 때마다 들여쓰기가 달라진다고
 * "문서가 바뀌었다"고 알리면 재검토 큐가 잡음으로 가득 차고, 그러면 진짜 개정도 함께 묻힌다.
 */
class ContentFingerprintTest {

    @Test
    @DisplayName("같은 내용이면 같은 지문")
    void 같은_내용은_같은_지문() {
        assertThat(ContentFingerprint.of("안전확인대상 전기용품은 신고하여야 한다."))
                .isEqualTo(ContentFingerprint.of("안전확인대상 전기용품은 신고하여야 한다."));
    }

    @Test
    @DisplayName("공백·줄바꿈 차이는 변경이 아니다")
    void 공백_차이는_무시한다() {
        String original = "안전확인대상 전기용품은\n신고하여야 한다.";
        String reformatted = "  안전확인대상   전기용품은\n\n\t신고하여야 한다.  ";

        assertThat(ContentFingerprint.of(original))
                .isEqualTo(ContentFingerprint.of(reformatted));
    }

    @Test
    @DisplayName("글자가 하나라도 다르면 다른 지문")
    void 내용이_다르면_다른_지문() {
        assertThat(ContentFingerprint.of("정격전압 50V 초과"))
                .isNotEqualTo(ContentFingerprint.of("정격전압 60V 초과"));
    }

    @Test
    @DisplayName("본문이 없으면 지문도 없다 — 못 가져온 것과 빈 문서를 구별할 이유가 없다")
    void 빈_본문은_지문이_없다() {
        assertThat(ContentFingerprint.of(null)).isNull();
        assertThat(ContentFingerprint.of("")).isNull();
        assertThat(ContentFingerprint.of("   \n\t ")).isNull();
    }

    @Test
    @DisplayName("지문은 SHA-256 16진 문자열이다")
    void 지문_형식() {
        assertThat(ContentFingerprint.of("내용"))
                .hasSize(64)
                .matches("[0-9a-f]{64}");
    }
}
