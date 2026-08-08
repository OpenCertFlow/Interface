package io.opencertflow.cli;

import io.opencertflow.diagnosis.application.port.out.RuleFileSourcePort.RuleSetFile;
import io.opencertflow.diagnosis.domain.rule.Rule;
import io.opencertflow.diagnosis.domain.rule.RuleConsistencyChecker;
import io.opencertflow.diagnosis.domain.rule.RuleConsistencyChecker.Finding;
import io.opencertflow.diagnosis.domain.rule.RuleConsistencyChecker.Severity;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;
import picocli.CommandLine.Model.CommandSpec;

/**
 * 룰 디렉터리를 검증한다. 커뮤니티 기여의 1차 관문이며, CI에서도 같은 명령을 돌린다.
 *
 * <p>세 겹으로 본다.
 * <ol>
 *   <li><b>구조</b> — JSON Schema. 필드 누락, 잘못된 열거값, 오타난 타입.
 *   <li><b>값 타입</b> — {@code RuleJsonCodec}. 속성이 기대하는 타입과 값이 맞는지.
 *   <li><b>의미</b> — {@code RuleConsistencyChecker}. 절대 발동하지 않는 룰, 중복 코드,
 *       효과 없는 룰, 아무 룰도 보지 않는 속성.
 * </ol>
 *
 * <p>종료 코드: 0 정상, 1 오류 발견, 2 파일을 읽지 못함. CI가 이 값으로 판단한다.
 */
@Command(
        name = "validate",
        mixinStandardHelpOptions = true,
        description = "룰 디렉터리의 구조·값 타입·의미를 검증한다.")
final class ValidateCommand implements Callable<Integer> {

    private static final int OK = 0;
    private static final int FINDINGS = 1;
    private static final int UNREADABLE = 2;

    @Parameters(
            index = "0",
            arity = "0..1",
            paramLabel = "<디렉터리>",
            defaultValue = "rules",
            description = "룰 YAML이 있는 디렉터리 (기본: ${DEFAULT-VALUE})")
    private String directory;

    @Option(
            names = {"-w", "--fail-on-warning"},
            description = "경고도 실패로 취급한다. CI에서 켜는 것을 권장한다.")
    private boolean failOnWarning;

    @Option(names = {"-q", "--quiet"}, description = "지적 사항만 출력한다.")
    private boolean quiet;

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
            return UNREADABLE;
        }

        if (ruleSets.isEmpty()) {
            err.println("✖ %s 아래에서 룰 파일(*.yaml)을 찾지 못했습니다.".formatted(directory));
            return UNREADABLE;
        }

        int errors = 0;
        int warnings = 0;

        for (RuleSetFile ruleSet : ruleSets) {
            String label = "%s v%d".formatted(ruleSet.productGroup(), ruleSet.version());

            List<Rule> rules;
            try {
                rules = reader.toDomainRules(ruleSet);
            } catch (RuntimeException e) {
                err.println("✖ %s — %s".formatted(label, e.getMessage()));
                err.println("  (%s)".formatted(ruleSet.origin()));
                errors++;
                continue;
            }

            List<Finding> findings = new ArrayList<>(RuleConsistencyChecker.check(rules));
            long ruleErrors = findings.stream().filter(f -> f.severity() == Severity.ERROR).count();
            long ruleWarnings = findings.size() - ruleErrors;
            errors += (int) ruleErrors;
            warnings += (int) ruleWarnings;

            if (findings.isEmpty()) {
                if (!quiet) {
                    out.println("✔ %-34s 룰 %2d개 · 이상 없음".formatted(label, rules.size()));
                }
                continue;
            }

            out.println("%s %-34s 룰 %2d개 · 오류 %d · 경고 %d".formatted(
                    ruleErrors > 0 ? "✖" : "!", label, rules.size(), ruleErrors, ruleWarnings));
            findings.stream()
                    .sorted((a, b) -> a.severity().compareTo(b.severity()))
                    .forEach(f -> out.println("    %-7s %-14s %s".formatted(
                            f.severity(),
                            f.ruleCode() == null || f.ruleCode().isBlank() ? "(룰셋)" : f.ruleCode(),
                            f.message())));
        }

        if (!quiet) {
            out.println();
            out.println("룰셋 %d개 검사 — 오류 %d · 경고 %d".formatted(ruleSets.size(), errors, warnings));
        }

        if (errors > 0) {
            return FINDINGS;
        }
        return (failOnWarning && warnings > 0) ? FINDINGS : OK;
    }
}
