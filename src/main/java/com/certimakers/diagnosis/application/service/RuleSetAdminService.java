package com.certimakers.diagnosis.application.service;

import com.certimakers.common.application.annotation.UseCase;
import com.certimakers.common.application.support.BlockingBridge;
import com.certimakers.common.domain.error.BusinessException;
import com.certimakers.diagnosis.application.port.in.ManageRuleSetUseCase;
import com.certimakers.diagnosis.application.port.out.RuleDefinitionValidatorPort;
import com.certimakers.diagnosis.application.port.out.RuleDefinitionValidatorPort.Definition;
import com.certimakers.diagnosis.application.port.out.RuleDefinitionValidatorPort.Issue;
import com.certimakers.diagnosis.application.port.out.RuleSetAdminPort;
import com.certimakers.diagnosis.application.port.out.RuleSetAdminPort.NewRule;
import com.certimakers.diagnosis.application.port.out.RuleSetAdminPort.NewRuleSet;
import com.certimakers.diagnosis.domain.model.ProductGroup;
import java.util.List;
import reactor.core.publisher.Mono;

/**
 * {@link ManageRuleSetUseCase} 구현. 조회는 그대로 내려보내고, 저장·배포는 <b>검증을 먼저</b> 건다.
 *
 * <p>블로킹 영속성 호출은 모두 {@link BlockingBridge}를 통과한다(ADR-0002). 검증은 순수 파싱이라
 * 블로킹이 아니지만, 저장 직전에 함께 수행하기 위해 같은 체인 안에서 처리한다.
 */
@UseCase
public class RuleSetAdminService implements ManageRuleSetUseCase {

    private final RuleSetAdminPort ruleSetAdminPort;
    private final RuleDefinitionValidatorPort validator;
    private final BlockingBridge blockingBridge;

    public RuleSetAdminService(
            RuleSetAdminPort ruleSetAdminPort,
            RuleDefinitionValidatorPort validator,
            BlockingBridge blockingBridge) {
        this.ruleSetAdminPort = ruleSetAdminPort;
        this.validator = validator;
        this.blockingBridge = blockingBridge;
    }

    @Override
    public Mono<List<RuleSetSummary>> list() {
        return blockingBridge.mono(() -> ruleSetAdminPort.findAllSummaries().stream()
                .map(s -> new RuleSetSummary(
                        s.id(), s.productGroup(), s.version(), s.active(),
                        s.activatedAt(), s.ruleCount()))
                .toList());
    }

    @Override
    public Mono<RuleSetDetail> get(Long ruleSetId) {
        return blockingBridge.mono(() -> ruleSetAdminPort.findDetail(ruleSetId).orElse(null))
                .switchIfEmpty(Mono.error(BusinessException.invalid("룰셋을 찾을 수 없습니다: " + ruleSetId)))
                .map(d -> new RuleSetDetail(
                        d.id(), d.productGroup(), d.version(), d.active(), d.activatedAt(),
                        d.rules().stream()
                                .map(r -> new RuleLine(
                                        r.ruleCode(), r.priority(), r.conditionJson(),
                                        r.effectsJson(), r.description()))
                                .toList()));
    }

    @Override
    public Mono<ValidationResult> validate(List<RuleDraft> rules) {
        return Mono.fromSupplier(() -> runValidation(rules));
    }

    @Override
    public Mono<Long> createDraft(CreateRuleSetCommand command) {
        return Mono.fromSupplier(() -> {
            ProductGroup productGroup = parseProductGroup(command.productGroup());
            ValidationResult result = runValidation(command.rules());
            if (!result.valid()) {
                throw BusinessException.invalid(
                        "룰 정의가 올바르지 않습니다: " + describe(result.issues()));
            }
            return productGroup;
        }).flatMap(productGroup -> blockingBridge.mono(() -> {
            int version = ruleSetAdminPort.nextVersion(productGroup);
            List<NewRule> rules = command.rules().stream()
                    .map(r -> new NewRule(
                            r.ruleCode(), r.priority(), r.conditionJson(),
                            r.effectsJson(), r.description()))
                    .toList();
            return ruleSetAdminPort.saveDraft(new NewRuleSet(productGroup, version, rules));
        }));
    }

    @Override
    public Mono<Void> activate(Long ruleSetId) {
        return blockingBridge.mono(() -> ruleSetAdminPort.activate(ruleSetId))
                .flatMap(activated -> Boolean.TRUE.equals(activated)
                        ? Mono.empty()
                        : Mono.error(BusinessException.invalid("룰셋을 찾을 수 없습니다: " + ruleSetId)));
    }

    // ── helpers ──────────────────────────────────────────────────

    private ValidationResult runValidation(List<RuleDraft> rules) {
        if (rules == null || rules.isEmpty()) {
            return new ValidationResult(false,
                    List.of(new RuleIssue("-", "룰이 하나 이상 필요합니다.")));
        }
        List<Definition> definitions = rules.stream()
                .map(r -> new Definition(r.ruleCode(), r.conditionJson(), r.effectsJson()))
                .toList();
        List<Issue> issues = validator.validate(definitions);
        List<RuleIssue> mapped = issues.stream()
                .map(i -> new RuleIssue(i.ruleCode(), i.message()))
                .toList();
        return new ValidationResult(mapped.isEmpty(), mapped);
    }

    private ProductGroup parseProductGroup(String raw) {
        if (raw == null || raw.isBlank()) {
            throw BusinessException.invalid("productGroup 값이 필요합니다.");
        }
        try {
            return ProductGroup.valueOf(raw);
        } catch (IllegalArgumentException e) {
            throw BusinessException.invalid("productGroup 값이 올바르지 않습니다: " + raw);
        }
    }

    private String describe(List<RuleIssue> issues) {
        return issues.stream()
                .map(i -> "[%s] %s".formatted(i.ruleCode(), i.message()))
                .reduce((a, b) -> a + "; " + b)
                .orElse("");
    }
}
