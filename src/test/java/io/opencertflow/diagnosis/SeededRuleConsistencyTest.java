package io.opencertflow.diagnosis;

import static org.assertj.core.api.Assertions.assertThat;

import io.opencertflow.diagnosis.application.port.out.LoadRuleSetPort;
import io.opencertflow.diagnosis.domain.model.ProductGroup;
import io.opencertflow.diagnosis.domain.rule.RuleConsistencyChecker;
import io.opencertflow.diagnosis.domain.rule.RuleSet;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * 저장소에 시드된 <b>실제</b> 룰셋이 스스로 모순되지 않는지 확인한다.
 *
 * <p>단위 테스트는 검사기가 인위적인 예제를 잡는지 본다. 이 테스트는 그 검사기를 우리 룰에
 * 들이대 본다 — 누군가 시드 SQL에 절대 발동하지 않는 룰을 넣으면 여기서 걸린다.
 *
 * <p>동시에 <b>거짓 경보가 없다는 증거</b>이기도 하다. 잘 쓴 룰에 경고가 붙는 검사기는 곧 무시되고,
 * 그러면 진짜 문제도 함께 묻힌다.
 */
@SpringBootTest
@ActiveProfiles("local")
@Testcontainers
class SeededRuleConsistencyTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    LoadRuleSetPort loadRuleSetPort;

    @ParameterizedTest
    @EnumSource(ProductGroup.class)
    @DisplayName("시드된 활성 룰셋에 정합성 오류가 없다")
    void 시드_룰셋은_정합적이다(ProductGroup group) {
        RuleSet ruleSet = loadRuleSetPort.loadActive(group);
        if (ruleSet == null) {
            return; // 아직 룰셋이 없는 제품군은 검사 대상이 아니다
        }

        List<RuleConsistencyChecker.Finding> errors =
                RuleConsistencyChecker.check(ruleSet.inPriorityOrder()).stream()
                        .filter(f -> f.severity() == RuleConsistencyChecker.Severity.ERROR)
                        .toList();

        assertThat(errors)
                .as("%s 룰셋의 정합성 오류", group)
                .isEmpty();
    }
}
