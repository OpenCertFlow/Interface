package com.certimakers.diagnosis.adapter.out.persistence.rule;

import com.certimakers.diagnosis.application.port.out.RuleDefinitionValidatorPort;
import com.certimakers.diagnosis.domain.rule.Rule;
import com.certimakers.diagnosis.domain.rule.RuleCode;
import com.certimakers.diagnosis.domain.rule.RuleConsistencyChecker;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * {@link RuleDefinitionValidatorPort} 구현. {@link RuleJsonCodec}으로 condition/effects를 실제
 * 파싱해 보고, 실패하면 그 룰의 오류로 수집한다.
 *
 * <p>파싱이 곧 검증이다 — 코덱이 도메인 트리로 되돌리지 못하는 JSON은 진단 시점에도 되돌리지 못해
 * 룰셋 로딩이 깨진다. 배포 전에 같은 코덱으로 미리 돌려, 그 실패를 활성화 전에 잡는다.
 */
@Component
public class RuleDefinitionValidatorAdapter implements RuleDefinitionValidatorPort {

    private final RuleJsonCodec codec;

    public RuleDefinitionValidatorAdapter(RuleJsonCodec codec) {
        this.codec = codec;
    }

    @Override
    public List<Issue> validate(List<Definition> definitions) {
        List<Issue> issues = new ArrayList<>();
        for (Definition definition : definitions) {
            validateOne(definition, issues);
        }
        return issues;
    }

    @Override
    public List<ConsistencyIssue> checkConsistency(List<Definition> definitions) {
        List<Rule> parsed = new ArrayList<>();
        for (Definition definition : definitions) {
            try {
                parsed.add(new Rule(
                        RuleCode.of(definition.ruleCode()),
                        0, // 우선순위는 정합성 판단에 쓰이지 않는다
                        codec.parseCondition(definition.conditionJson()),
                        codec.parseEffects(definition.effectsJson())));
            } catch (RuntimeException e) {
                // 문법 오류는 validate()가 이미 보고했다. 여기서 또 지적하지 않는다.
            }
        }
        return RuleConsistencyChecker.check(parsed).stream()
                .map(finding -> new ConsistencyIssue(
                        finding.severity().name(), finding.ruleCode(),
                        finding.kind(), finding.message()))
                .toList();
    }

    private void validateOne(Definition definition, List<Issue> issues) {
        String code = definition.ruleCode() == null || definition.ruleCode().isBlank()
                ? "-" : definition.ruleCode();
        if (definition.conditionJson() == null || definition.conditionJson().isBlank()) {
            issues.add(new Issue(code, "condition이 비어 있습니다."));
        } else {
            try {
                codec.parseCondition(definition.conditionJson());
            } catch (RuntimeException e) {
                issues.add(new Issue(code, "condition 파싱 실패: " + rootMessage(e)));
            }
        }
        if (definition.effectsJson() == null || definition.effectsJson().isBlank()) {
            issues.add(new Issue(code, "effects가 비어 있습니다."));
        } else {
            try {
                codec.parseEffects(definition.effectsJson());
            } catch (RuntimeException e) {
                issues.add(new Issue(code, "effects 파싱 실패: " + rootMessage(e)));
            }
        }
    }

    private String rootMessage(Throwable e) {
        Throwable cursor = e;
        while (cursor.getCause() != null && cursor.getCause() != cursor) {
            cursor = cursor.getCause();
        }
        String message = cursor.getMessage();
        return message != null ? message : cursor.getClass().getSimpleName();
    }
}
