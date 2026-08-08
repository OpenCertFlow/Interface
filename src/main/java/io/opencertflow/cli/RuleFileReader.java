package io.opencertflow.cli;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.opencertflow.diagnosis.adapter.out.persistence.rule.RuleJsonCodec;
import io.opencertflow.diagnosis.adapter.out.rulefile.RuleFileProperties;
import io.opencertflow.diagnosis.adapter.out.rulefile.YamlRuleFileAdapter;
import io.opencertflow.diagnosis.application.port.out.RuleFileSourcePort.RuleFile;
import io.opencertflow.diagnosis.application.port.out.RuleFileSourcePort.RuleSetFile;
import io.opencertflow.diagnosis.domain.rule.Rule;
import io.opencertflow.diagnosis.domain.rule.RuleCode;
import java.util.ArrayList;
import java.util.List;

/**
 * CLI가 룰 디렉터리를 읽어 도메인 {@link Rule}로 되돌린다.
 *
 * <p>서버가 쓰는 {@link YamlRuleFileAdapter}·{@link RuleJsonCodec}을 그대로 재사용한다 — CLI가
 * 별도 파서를 갖게 되면 "CLI는 통과했는데 서버가 거부하는" 파일이 생긴다. 두 경로가 같은 코드를
 * 지나야 검증이 의미를 갖는다.
 *
 * <p>스프링 컨텍스트 없이 {@code new}로 조립한다. 두 클래스 모두 상태 없는 순수 변환기다.
 */
final class RuleFileReader {

    private final ObjectMapper mapper = new ObjectMapper();
    private final RuleJsonCodec codec = new RuleJsonCodec(mapper);

    /** 디렉터리 하위의 모든 룰셋 YAML을 읽는다. 스키마 위반이면 예외가 난다. */
    List<RuleSetFile> readRuleSets(String directory) {
        YamlRuleFileAdapter adapter =
                new YamlRuleFileAdapter(new RuleFileProperties(false, directory, ""));
        return adapter.loadRuleSets();
    }

    /**
     * 파일의 룰을 도메인 트리로 파싱한다. 여기서 실패하면 스키마는 통과했지만 값 타입이 어긋난
     * 것이다(예: {@code RATED_VOLTAGE}에 문자열).
     */
    List<Rule> toDomainRules(RuleSetFile ruleSet) {
        List<Rule> rules = new ArrayList<>(ruleSet.rules().size());
        for (RuleFile rule : ruleSet.rules()) {
            rules.add(new Rule(
                    RuleCode.of(rule.code()),
                    rule.priority(),
                    codec.parseCondition(rule.conditionJson()),
                    codec.parseEffects(rule.effectsJson())));
        }
        return rules;
    }
}
