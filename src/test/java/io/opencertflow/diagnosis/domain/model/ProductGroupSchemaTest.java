package io.opencertflow.diagnosis.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * 제품군별 입력 스키마. 앱이 이 정의를 그대로 그리므로, 스키마가 룰이 기대하는 입력과 어긋나면
 * 사용자는 필요한 값을 입력할 방법 자체가 없어진다.
 */
class ProductGroupSchemaTest {

    @Test
    @DisplayName("전기방석만 발열 항목을 묻는다 — 드라이기 화면에 표면온도 칸이 뜨면 안 된다")
    void 발열_항목은_전기방석에만_있다() {
        List<String> heatingCodes =
                List.of("bodyContactType", "controllerStatus", "maxSurfaceTemperatureCelsius");

        assertThat(codesOf(ProductGroup.ELECTRIC_HEATING_PAD)).containsAll(heatingCodes);
        assertThat(codesOf(ProductGroup.SMALL_APPLIANCE)).doesNotContainAnyElementsOf(heatingCodes);

        assertThat(ProductGroup.ELECTRIC_HEATING_PAD.requiresHeatingSpec()).isTrue();
        assertThat(ProductGroup.SMALL_APPLIANCE.requiresHeatingSpec()).isFalse();
    }

    @Test
    @DisplayName("표면온도는 선택 입력이다 — 측정값을 몰라도 진단은 받을 수 있어야 한다")
    void 표면온도는_선택_입력이다() {
        InputField surfaceTemp = fieldOf(ProductGroup.ELECTRIC_HEATING_PAD, "maxSurfaceTemperatureCelsius");

        assertThat(surfaceTemp.required()).isFalse();
        assertThat(surfaceTemp.type()).isEqualTo(InputFieldType.INTEGER);
    }

    @Test
    @DisplayName("전압·소비전력은 전기 사용에 의존한다 — 전기를 안 쓰면 물을 이유가 없다")
    void 전압은_전기사용에_의존한다() {
        for (ProductGroup group : ProductGroup.values()) {
            assertThat(fieldOf(group, "ratedVoltage").dependsOn()).isEqualTo("usesElectricity");
            assertThat(fieldOf(group, "powerConsumption").dependsOn()).isEqualTo("usesElectricity");
        }
    }

    @ParameterizedTest
    @EnumSource(ProductGroup.class)
    @DisplayName("모든 제품군은 진단에 필요한 공통 항목을 갖는다")
    void 모든_제품군이_공통_항목을_갖는다(ProductGroup group) {
        assertThat(codesOf(group)).contains(
                "productName", "usesElectricity", "hasBattery",
                "targetUser", "salesChannel", "materials", "heldDocuments");
        assertThat(group.displayName()).isNotBlank();
        assertThat(group.description()).isNotBlank();
    }

    @ParameterizedTest
    @EnumSource(ProductGroup.class)
    @DisplayName("항목 코드는 유일하고, 의존 대상은 같은 스키마 안에 존재한다")
    void 스키마가_자기_모순이_없다(ProductGroup group) {
        List<String> codes = codesOf(group);
        assertThat(codes).doesNotHaveDuplicates();

        // dependsOn이 없는 항목을 가리키면 앱이 노출 조건을 판단할 수 없다.
        group.inputFields().stream()
                .map(InputField::dependsOn)
                .filter(java.util.Objects::nonNull)
                .forEach(dependency -> assertThat(codes)
                        .as("의존 대상 '%s'이 스키마에 있어야 한다", dependency)
                        .contains(dependency));
    }

    @ParameterizedTest
    @EnumSource(ProductGroup.class)
    @DisplayName("선택형 항목은 보기를 갖고, 그 외 항목은 갖지 않는다")
    void 선택형_항목만_보기를_갖는다(ProductGroup group) {
        for (InputField field : group.inputFields()) {
            boolean isSelect = field.type() == InputFieldType.SINGLE_SELECT
                    || field.type() == InputFieldType.MULTI_SELECT;

            if (isSelect) {
                assertThat(field.options())
                        .as("%s는 선택형이므로 보기가 있어야 한다", field.code())
                        .isNotEmpty();
            } else {
                assertThat(field.options())
                        .as("%s는 선택형이 아니므로 보기가 없어야 한다", field.code())
                        .isEmpty();
            }
        }
    }

    private static List<String> codesOf(ProductGroup group) {
        return group.inputFields().stream().map(InputField::code).toList();
    }

    private static InputField fieldOf(ProductGroup group, String code) {
        return group.inputFields().stream()
                .filter(field -> field.code().equals(code))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "%s 스키마에 '%s' 항목이 없습니다".formatted(group, code)));
    }
}
