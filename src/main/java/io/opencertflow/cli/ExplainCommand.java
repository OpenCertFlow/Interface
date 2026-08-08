package io.opencertflow.cli;

import io.opencertflow.diagnosis.application.port.out.RuleFileSourcePort.RuleSetFile;
import io.opencertflow.diagnosis.domain.rule.AddCandidate;
import io.opencertflow.diagnosis.domain.rule.AddLabelingCheck;
import io.opencertflow.diagnosis.domain.rule.AllOf;
import io.opencertflow.diagnosis.domain.rule.AnyOf;
import io.opencertflow.diagnosis.domain.rule.AttributeMatch;
import io.opencertflow.diagnosis.domain.rule.Condition;
import io.opencertflow.diagnosis.domain.rule.Effect;
import io.opencertflow.diagnosis.domain.rule.FlagExpertReview;
import io.opencertflow.diagnosis.domain.rule.Not;
import io.opencertflow.diagnosis.domain.rule.RequireDocument;
import io.opencertflow.diagnosis.domain.rule.Rule;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

/**
 * 룰 하나가 "언제 켜지고 무엇을 하는지"를 사람의 언어로 보여 준다.
 *
 * <p>룰 코드({@code R-EH-004})만으로는 왜 그 결과가 나왔는지 설명할 수 없다. 상담사와 소공인이
 * 읽을 수 있어야 룰이 검토 가능한 자산이 된다 — 그것이 이 명령의 목적이다.
 */
@Command(
        name = "explain",
        mixinStandardHelpOptions = true,
        description = "룰의 발동 조건과 효과를 트리로 풀어서 보여 준다.")
final class ExplainCommand implements Callable<Integer> {

    @Parameters(
            index = "0",
            arity = "0..1",
            paramLabel = "<룰코드>",
            description = "설명할 룰 코드. 생략하면 전체 목록을 보여 준다. 예: R-EH-004")
    private String ruleCode;

    @Option(
            names = {"-d", "--dir"},
            paramLabel = "<디렉터리>",
            defaultValue = "rules",
            description = "룰 YAML이 있는 디렉터리 (기본: ${DEFAULT-VALUE})")
    private String directory;

    @Spec
    private CommandSpec spec;

    @Override
    public Integer call() {
        PrintWriter out = spec.commandLine().getOut();
        PrintWriter err = spec.commandLine().getErr();
        RuleFileReader reader = new RuleFileReader();

        List<RuleSetFile> ruleSets;
        try {
            ruleSets = reader.readRuleSets(directory);
        } catch (RuntimeException e) {
            err.println("✖ " + e.getMessage());
            return 2;
        }

        boolean found = false;
        for (RuleSetFile ruleSet : ruleSets) {
            List<Rule> rules = reader.toDomainRules(ruleSet);

            if (ruleCode == null) {
                out.println("── %s v%d ─────────────────────────".formatted(
                        ruleSet.productGroup(), ruleSet.version()));
                ruleSet.rules().forEach(r -> out.println("  %-12s p%-3d %s".formatted(
                        r.code(), r.priority(), r.description() == null ? "" : r.description())));
                out.println();
                found = true;
                continue;
            }

            for (int i = 0; i < rules.size(); i++) {
                Rule rule = rules.get(i);
                if (!rule.code().value().equalsIgnoreCase(ruleCode)) {
                    continue;
                }
                found = true;
                print(out, ruleSet, rule, ruleSet.rules().get(i).description());
            }
        }

        if (!found) {
            err.println("✖ 룰을 찾지 못했습니다: " + ruleCode);
            return 1;
        }
        return 0;
    }

    private void print(PrintWriter out, RuleSetFile ruleSet, Rule rule, String description) {
        out.println("%s  (%s v%d, 우선순위 %d)".formatted(
                rule.code().value(), ruleSet.productGroup(), ruleSet.version(), rule.priority()));
        if (description != null && !description.isBlank()) {
            out.println("  " + description);
        }
        out.println();
        out.println("  다음일 때 발동한다:");
        renderCondition(out, rule.condition(), "    ");
        out.println();
        out.println("  그러면:");
        for (Effect effect : rule.effects()) {
            out.println("    • " + describe(effect));
        }
        out.println();
    }

    private void renderCondition(PrintWriter out, Condition condition, String indent) {
        if (condition instanceof AllOf allOf) {
            out.println(indent + "모두 참:");
            allOf.conditions().forEach(c -> renderCondition(out, c, indent + "  "));
        } else if (condition instanceof AnyOf anyOf) {
            out.println(indent + "하나라도 참:");
            anyOf.conditions().forEach(c -> renderCondition(out, c, indent + "  "));
        } else if (condition instanceof Not not) {
            out.println(indent + "다음이 아님:");
            renderCondition(out, not.condition(), indent + "  ");
        } else if (condition instanceof AttributeMatch match) {
            out.println(indent + "- " + describe(match));
        }
    }

    private String describe(AttributeMatch match) {
        String value = match.value() == null
                ? "값 없음"
                : match.value() instanceof Collection<?> items
                        ? items.stream().map(String::valueOf).reduce((a, b) -> a + ", " + b).orElse("")
                        : String.valueOf(match.value());
        return "%s %s %s".formatted(match.attribute(), symbol(match.operator().name()), value);
    }

    /** 연산자를 기호로 바꾼다. {@code GTE}보다 {@code >=}가 빨리 읽힌다. */
    private String symbol(String operator) {
        return switch (operator) {
            case "EQ" -> "=";
            case "NEQ" -> "≠";
            case "GT" -> ">";
            case "GTE" -> "≥";
            case "LT" -> "<";
            case "LTE" -> "≤";
            case "IN" -> "∈ {";
            case "CONTAINS" -> "포함";
            default -> operator;
        };
    }

    private String describe(Effect effect) {
        if (effect instanceof AddCandidate add) {
            return "인증 검토 후보 추가: %s (%s)".formatted(add.schemeCode().value(), add.type());
        }
        if (effect instanceof RequireDocument require) {
            return "서류 요구: %s (%s)".formatted(require.documentCode().value(), require.requirement());
        }
        if (effect instanceof AddLabelingCheck label) {
            return "표시·라벨 확인: " + label.label();
        }
        if (effect instanceof FlagExpertReview flag) {
            return "전문가 확인 필요 [%s]: %s".formatted(flag.reason(), flag.question());
        }
        return effect.toString();
    }
}
