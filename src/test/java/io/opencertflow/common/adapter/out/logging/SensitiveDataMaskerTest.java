package io.opencertflow.common.adapter.out.logging;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 로그 개인정보 마스킹 검증. 형식에 맞는 값만 가리되 원문이 남지 않아야 한다. */
class SensitiveDataMaskerTest {

    @Test
    @DisplayName("휴대폰 번호는 가운데를 가리고 끝 4자리만 남긴다")
    void 휴대폰_마스킹() {
        assertThat(SensitiveDataMasker.mask("연락처 010-1234-5678 입니다"))
                .isEqualTo("연락처 010-****-5678 입니다")
                .doesNotContain("1234");
        assertThat(SensitiveDataMasker.mask("01098765432")).contains("010-****-5432");
    }

    @Test
    @DisplayName("이메일은 앞 한 글자만 남기고 가린다")
    void 이메일_마스킹() {
        assertThat(SensitiveDataMasker.mask("gildong.hong@example.com 로 발송"))
                .isEqualTo("g***@example.com 로 발송")
                .doesNotContain("ildong");
    }

    @Test
    @DisplayName("주민등록번호는 뒷 7자리를 가린다")
    void 주민번호_마스킹() {
        assertThat(SensitiveDataMasker.mask("주민 900101-1234567"))
                .isEqualTo("주민 900101-*******")
                .doesNotContain("1234567");
    }

    @Test
    @DisplayName("민감정보가 없으면 원문을 그대로 둔다")
    void 일반_문자열은_그대로() {
        String plain = "진단 완료 diagnosisId=abc-123 status=COMPLETED";
        assertThat(SensitiveDataMasker.mask(plain)).isEqualTo(plain);
    }

    @Test
    @DisplayName("null·빈 문자열도 안전하게 처리한다")
    void null과_빈문자열() {
        assertThat(SensitiveDataMasker.mask(null)).isNull();
        assertThat(SensitiveDataMasker.mask("")).isEmpty();
    }
}
