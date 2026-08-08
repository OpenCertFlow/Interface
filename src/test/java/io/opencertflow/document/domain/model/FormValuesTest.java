package io.opencertflow.document.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.opencertflow.common.domain.error.BusinessException;
import io.opencertflow.document.domain.error.DocumentErrorCode;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * 양식 값 검증. 검증되지 않은 값이 담긴 {@link FormValues}가 <b>존재할 수 없어야</b> 한다는 것이
 * 이 값 객체의 계약이다.
 */
class FormValuesTest {

    /** 자기적합성 선언서의 필수 항목을 모두 채운 값. */
    private static Map<String, String> validSelfDeclaration() {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("companyName", "OpenCertFlow");
        values.put("businessNumber", "123-45-67890");
        values.put("representative", "홍길동");
        values.put("productName", "가정용 헤어드라이어");
        values.put("modelName", "CM-100");
        values.put("ratedVoltage", "220");
        values.put("powerConsumption", "1200");
        values.put("declarationDate", "2026-08-10");
        return values;
    }

    @Test
    @DisplayName("필수 항목을 모두 채우면 생성된다")
    void 필수_항목을_채우면_생성된다() {
        FormValues values = new FormValues(DocumentTemplate.SELF_DECLARATION, validSelfDeclaration());

        assertThat(values.valueOf("companyName")).contains("OpenCertFlow");
        assertThat(values.values()).hasSize(8);
    }

    @Nested
    @DisplayName("필수 항목 검증")
    class RequiredFields {

        @Test
        @DisplayName("필수 항목이 비면 어떤 항목인지 알려 준다")
        void 필수_항목이_비면_거부한다() {
            Map<String, String> incomplete = validSelfDeclaration();
            incomplete.remove("businessNumber");

            assertThatThrownBy(() -> new FormValues(DocumentTemplate.SELF_DECLARATION, incomplete))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(error -> {
                        BusinessException business = (BusinessException) error;
                        assertThat(business.errorCode())
                                .isEqualTo(DocumentErrorCode.REQUIRED_FIELD_MISSING);
                        assertThat(business.getMessage()).contains("사업자등록번호");
                    });
        }

        @Test
        @DisplayName("빈 문자열은 입력하지 않은 것으로 본다")
        void 빈_문자열은_미입력으로_본다() {
            Map<String, String> blanked = validSelfDeclaration();
            blanked.put("representative", "   ");

            assertThatThrownBy(() -> new FormValues(DocumentTemplate.SELF_DECLARATION, blanked))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("선택 항목은 없어도 된다")
        void 선택_항목은_없어도_된다() {
            assertThatCode(() -> new FormValues(DocumentTemplate.SELF_DECLARATION, validSelfDeclaration()))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("모르는 항목은 조용히 버리지 않는다")
    class UnknownFields {

        @Test
        @DisplayName("양식에 없는 항목을 보내면 거부한다")
        void 양식에_없는_항목은_거부한다() {
            Map<String, String> withTypo = validSelfDeclaration();
            withTypo.put("compnayName", "오타난 항목");

            assertThatThrownBy(() -> new FormValues(DocumentTemplate.SELF_DECLARATION, withTypo))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(error -> assertThat(((BusinessException) error).errorCode())
                            .isEqualTo(DocumentErrorCode.UNKNOWN_FIELD));
        }
    }

    @Nested
    @DisplayName("형식 검증")
    class ValueFormat {

        @Test
        @DisplayName("날짜 항목은 YYYY-MM-DD여야 한다")
        void 날짜_형식을_강제한다() {
            Map<String, String> badDate = validSelfDeclaration();
            badDate.put("declarationDate", "2026/08/10");

            assertThatThrownBy(() -> new FormValues(DocumentTemplate.SELF_DECLARATION, badDate))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(error -> assertThat(((BusinessException) error).errorCode())
                            .isEqualTo(DocumentErrorCode.INVALID_FIELD_VALUE));
        }

        @Test
        @DisplayName("숫자 항목에 문자를 넣으면 거부한다")
        void 숫자_형식을_강제한다() {
            Map<String, String> badNumber = validSelfDeclaration();
            badNumber.put("ratedVoltage", "220V");

            assertThatThrownBy(() -> new FormValues(DocumentTemplate.SELF_DECLARATION, badNumber))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("길이 상한을 넘으면 거부한다")
        void 길이_상한을_강제한다() {
            Map<String, String> tooLong = validSelfDeclaration();
            tooLong.put("companyName", "가".repeat(201));

            assertThatThrownBy(() -> new FormValues(DocumentTemplate.SELF_DECLARATION, tooLong))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Test
    @DisplayName("표시 순서는 양식 정의를 따른다 — 화면과 PDF가 어긋나지 않게")
    void 표시_순서는_양식_정의를_따른다() {
        // 입력 순서를 일부러 뒤집어 넣는다
        Map<String, String> shuffled = new LinkedHashMap<>();
        shuffled.put("declarationDate", "2026-08-10");
        shuffled.put("companyName", "OpenCertFlow");
        shuffled.put("businessNumber", "123-45-67890");
        shuffled.put("representative", "홍길동");
        shuffled.put("productName", "드라이어");
        shuffled.put("modelName", "CM-100");
        shuffled.put("ratedVoltage", "220");
        shuffled.put("powerConsumption", "1200");

        FormValues values = new FormValues(DocumentTemplate.SELF_DECLARATION, shuffled);

        assertThat(values.orderedLabelledValues())
                .extracting(Map.Entry::getKey)
                .startsWith("업체명", "사업자등록번호", "대표자명");
    }

    @ParameterizedTest
    @EnumSource(DocumentTemplate.class)
    @DisplayName("모든 양식은 항목 코드가 유일하고 필수 항목을 하나 이상 가진다")
    void 모든_양식이_일관된_정의를_가진다(DocumentTemplate template) {
        assertThat(template.fields())
                .extracting(FormField::code)
                .doesNotHaveDuplicates();
        assertThat(template.requiredFields()).isNotEmpty();
        assertThat(template.displayName()).isNotBlank();
        assertThat(template.description()).isNotBlank();
    }
}
