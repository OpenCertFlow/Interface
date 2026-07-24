package com.certimakers.diagnosis.application.service;

import com.certimakers.common.application.annotation.UseCase;
import com.certimakers.common.application.support.BlockingBridge;
import com.certimakers.common.domain.error.BusinessException;
import com.certimakers.diagnosis.application.port.in.GetProductGroupSchemaUseCase;
import com.certimakers.diagnosis.application.port.in.ManageProductGroupQuestionUseCase;
import com.certimakers.diagnosis.application.port.out.ProductGroupSchemaPort;
import com.certimakers.diagnosis.application.port.out.ProductGroupSchemaPort.OverridePatch;
import com.certimakers.diagnosis.application.port.out.ProductGroupSchemaPort.QuestionOverride;
import com.certimakers.diagnosis.domain.model.InputField;
import com.certimakers.diagnosis.domain.model.ProductGroup;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import reactor.core.publisher.Mono;

/**
 * 제품군 스키마 조회 + 질문 프레젠테이션 관리. enum(타입 계약)에 DB 오버라이드(프레젠테이션 델타)를
 * 얹어 <b>유효 스키마</b>를 만든다.
 *
 * <p>병합 규칙: 오버라이드 값이 null이면 enum 기본값을 쓴다. 오버라이드가 하나도 없으면 결과는
 * enum과 완전히 동일하다 — 그래서 기존 동작이 그대로 보존된다.
 *
 * <p>편집은 <b>이미 존재하는 코드</b>에만 허용한다. 코드·타입·의존은 진단 DTO·Attribute·룰과 묶여
 * 있어, 없는 코드를 만들면 룰이 읽을 수 없는 질문이 생기기 때문이다.
 */
@UseCase
public class ProductGroupSchemaService
        implements GetProductGroupSchemaUseCase, ManageProductGroupQuestionUseCase {

    private final ProductGroupSchemaPort schemaPort;
    private final BlockingBridge blockingBridge;

    public ProductGroupSchemaService(
            ProductGroupSchemaPort schemaPort, BlockingBridge blockingBridge) {
        this.schemaPort = schemaPort;
        this.blockingBridge = blockingBridge;
    }

    // ── 조회(공개) ────────────────────────────────────────────────

    @Override
    public Mono<List<ProductGroupSchemaView>> getAll() {
        return blockingBridge.mono(() -> {
            List<ProductGroupSchemaView> views = new ArrayList<>();
            for (ProductGroup group : ProductGroup.values()) {
                Map<String, QuestionOverride> overrides = overrideMap(group.name());
                List<OrderedField> ordered = new ArrayList<>();
                int index = 0;
                for (InputField field : group.inputFields()) {
                    QuestionOverride override = overrides.get(field.code());
                    if (override != null && !override.active()) {
                        index++;
                        continue; // 숨긴 항목은 조회 결과에서 제외한다
                    }
                    ordered.add(new OrderedField(toFieldView(field, override), order(override, index)));
                    index++;
                }
                List<FieldView> fields = ordered.stream()
                        .sorted(Comparator.comparingInt(OrderedField::order))
                        .map(OrderedField::view)
                        .toList();
                views.add(new ProductGroupSchemaView(
                        group.name(), group.displayName(), group.description(), fields));
            }
            return views;
        });
    }

    // ── 관리(ADMIN) ───────────────────────────────────────────────

    @Override
    public Mono<List<QuestionAdminView>> list(String productGroup) {
        // 입력 검증도 리액티브 체인 안에서 수행한다 — 동기 throw는 Mono.error가 아니라 즉시 예외라
        // 호출부의 오류 처리를 우회한다.
        return Mono.fromSupplier(() -> parseGroup(productGroup))
                .flatMap(group -> blockingBridge.mono(() -> {
                    Map<String, QuestionOverride> overrides = overrideMap(group.name());
                    List<QuestionAdminView> views = new ArrayList<>();
                    int index = 0;
                    for (InputField field : group.inputFields()) {
                        QuestionOverride override = overrides.get(field.code());
                        views.add(toAdminView(field, override, index));
                        index++;
                    }
                    return views;
                }));
    }

    @Override
    public Mono<Void> update(String productGroup, String code, UpdateQuestionCommand command) {
        return Mono.fromSupplier(() -> {
            ProductGroup group = parseGroup(productGroup);
            // 존재하지 않는 코드는 편집할 수 없다 — 룰이 읽을 수 없는 질문을 만들 수 없다.
            if (group.inputFields().stream().noneMatch(field -> field.code().equals(code))) {
                throw BusinessException.invalid(
                        "%s 스키마에 없는 항목입니다: %s".formatted(group.name(), code));
            }
            return group;
        }).flatMap(group -> {
            List<ProductGroupSchemaPort.Option> options = command.options() == null ? null
                    : command.options().stream()
                            .map(o -> new ProductGroupSchemaPort.Option(o.value(), o.label()))
                            .toList();
            OverridePatch patch = new OverridePatch(
                    command.label(), command.helpText(), command.required(),
                    command.displayOrder(), command.active(), options);
            return blockingBridge.run(() -> schemaPort.upsert(group.name(), code, patch));
        });
    }

    // ── 병합 헬퍼 ─────────────────────────────────────────────────

    private Map<String, QuestionOverride> overrideMap(String productGroup) {
        return schemaPort.loadOverrides(productGroup).stream()
                .collect(Collectors.toMap(QuestionOverride::code, Function.identity()));
    }

    private FieldView toFieldView(InputField field, QuestionOverride override) {
        return new FieldView(
                field.code(),
                value(override == null ? null : override.label(), field.label()),
                field.type().name(),
                requiredValue(override, field),
                field.dependsOn(),
                value(override == null ? null : override.helpText(), field.helpText()),
                options(override, field));
    }

    private QuestionAdminView toAdminView(InputField field, QuestionOverride override, int index) {
        return new QuestionAdminView(
                field.code(),
                value(override == null ? null : override.label(), field.label()),
                field.type().name(),
                requiredValue(override, field),
                field.dependsOn(),
                value(override == null ? null : override.helpText(), field.helpText()),
                order(override, index),
                override == null || override.active(),
                override != null,
                options(override, field).stream()
                        .map(o -> new Option(o.code(), o.label()))
                        .toList());
    }

    private List<OptionView> options(QuestionOverride override, InputField field) {
        if (override != null && override.options() != null) {
            return override.options().stream()
                    .map(o -> new OptionView(o.value(), o.label()))
                    .toList();
        }
        return field.options().stream()
                .map(o -> new OptionView(o.code(), o.label()))
                .toList();
    }

    private boolean requiredValue(QuestionOverride override, InputField field) {
        if (override != null && override.required() != null) {
            return override.required();
        }
        return field.required();
    }

    private int order(QuestionOverride override, int naturalIndex) {
        if (override != null && override.displayOrder() != null) {
            return override.displayOrder();
        }
        return naturalIndex;
    }

    private String value(String override, String fallback) {
        return override != null ? override : fallback;
    }

    private ProductGroup parseGroup(String raw) {
        if (raw == null || raw.isBlank()) {
            throw BusinessException.invalid("productGroup 값이 필요합니다.");
        }
        try {
            return ProductGroup.valueOf(raw);
        } catch (IllegalArgumentException e) {
            throw BusinessException.invalid("productGroup 값이 올바르지 않습니다: " + raw);
        }
    }

    private record OrderedField(FieldView view, int order) {
    }
}
