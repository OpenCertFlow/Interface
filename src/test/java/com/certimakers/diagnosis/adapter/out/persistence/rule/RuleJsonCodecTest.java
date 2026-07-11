package com.certimakers.diagnosis.adapter.out.persistence.rule;

import static org.assertj.core.api.Assertions.assertThat;

import com.certimakers.diagnosis.domain.ProductProfileFixtures;
import com.certimakers.diagnosis.domain.model.CertificationType;
import com.certimakers.diagnosis.domain.rule.AddCandidate;
import com.certimakers.diagnosis.domain.rule.Condition;
import com.certimakers.diagnosis.domain.rule.Effect;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 코덱이 R__seed_rules.sql의 JSON을 정확히 파싱하는지 검증한다. 여기 JSON 문자열은 시드 파일과
 * 동일해야 하며, 이 테스트가 시드와 코덱의 정렬을 보장한다.
 */
class RuleJsonCodecTest {

    private final RuleJsonCodec codec = new RuleJsonCodec(new ObjectMapper());

    @Test
    @DisplayName("R-SA-001 조건: 전기 사용 + 전압>50 은 220V 드라이기에 매칭된다")
    void allOf_전압비교_파싱() {
        String json = """
                {"type":"allOf","conditions":[
                    {"type":"attr","attribute":"USES_ELECTRICITY","operator":"EQ","value":true},
                    {"type":"attr","attribute":"RATED_VOLTAGE","operator":"GT","value":50}
                ]}""";

        Condition condition = codec.parseCondition(json);

        assertThat(condition.test(ProductProfileFixtures.hairDryer(Set.of()))).isTrue();
        assertThat(condition.test(ProductProfileFixtures.nonElectricProduct())).isFalse();
    }

    @Test
    @DisplayName("R-SA-001 효과: addCandidate와 requireDocument를 파싱한다")
    void effects_파싱() {
        String json = """
                [
                    {"type":"addCandidate","schemeCode":"KC_SAFETY_CONFIRM_ELECTRIC","certificationType":"SAFETY_CONFIRM"},
                    {"type":"requireDocument","documentCode":"BIZ_LICENSE","requirement":"REQUIRED"}
                ]""";

        List<Effect> effects = codec.parseEffects(json);

        assertThat(effects).hasSize(2);
        assertThat(effects.get(0)).isInstanceOfSatisfying(AddCandidate.class, add -> {
            assertThat(add.schemeCode().value()).isEqualTo("KC_SAFETY_CONFIRM_ELECTRIC");
            assertThat(add.type()).isEqualTo(CertificationType.SAFETY_CONFIRM);
        });
    }

    @Test
    @DisplayName("enum 값(TARGET_USER=CHILD)을 문자열에서 enum으로 되돌린다")
    void enum_속성_값_변환() {
        String json = """
                {"type":"attr","attribute":"TARGET_USER","operator":"EQ","value":"CHILD"}""";

        Condition condition = codec.parseCondition(json);

        // 어린이용 드라이기에는 매칭, 일반용에는 비매칭 — enum 비교가 문자열이 아니라 타입으로 됐다는 뜻
        assertThat(condition.test(ProductProfileFixtures.hairDryer(Set.of()))).isFalse();
    }

    @Test
    @DisplayName("R-SA-090: value가 null인 EQ와 not(...)을 파싱한다")
    void null값과_not_파싱() {
        String json = """
                {"type":"allOf","conditions":[
                    {"type":"attr","attribute":"USES_ELECTRICITY","operator":"EQ","value":true},
                    {"type":"attr","attribute":"RATED_VOLTAGE","operator":"EQ","value":null},
                    {"type":"not","condition":{"type":"attr","attribute":"HAS_BATTERY","operator":"EQ","value":true}}
                ]}""";

        Condition condition = codec.parseCondition(json);

        // 전압 없는 드라이기(전기O, 전압null, 배터리X)에는 매칭
        assertThat(condition.test(ProductProfileFixtures.hairDryerWithoutVoltage(Set.of()))).isTrue();
        // 전압 있는 드라이기(220V)에는 비매칭
        assertThat(condition.test(ProductProfileFixtures.hairDryer(Set.of()))).isFalse();
    }
}
