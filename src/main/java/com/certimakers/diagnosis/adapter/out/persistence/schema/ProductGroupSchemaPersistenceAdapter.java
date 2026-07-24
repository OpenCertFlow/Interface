package com.certimakers.diagnosis.adapter.out.persistence.schema;

import com.certimakers.common.adapter.out.persistence.annotation.PersistenceAdapter;
import com.certimakers.common.domain.error.BusinessException;
import com.certimakers.common.domain.error.CommonErrorCode;
import com.certimakers.common.domain.port.IdGenerator;
import com.certimakers.diagnosis.application.port.out.ProductGroupSchemaPort;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link ProductGroupSchemaPort} 구현. 프레젠테이션 오버라이드를 저장/조회하며, 선택 보기는
 * jsonb 문자열로 담고 여기서 {@link ObjectMapper}로 오간다 — 파싱 지식을 어댑터에 가둔다(ArchUnit).
 */
@PersistenceAdapter
public class ProductGroupSchemaPersistenceAdapter implements ProductGroupSchemaPort {

    private static final TypeReference<List<OptionJson>> OPTION_LIST = new TypeReference<>() {
    };

    private final ProductGroupQuestionOverrideJpaRepository repository;
    private final ObjectMapper objectMapper;
    private final IdGenerator idGenerator;

    public ProductGroupSchemaPersistenceAdapter(
            ProductGroupQuestionOverrideJpaRepository repository,
            ObjectMapper objectMapper, IdGenerator idGenerator) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.idGenerator = idGenerator;
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuestionOverride> loadOverrides(String productGroup) {
        return repository.findByProductGroup(productGroup).stream()
                .map(this::toOverride)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<QuestionOverride> findOverride(String productGroup, String code) {
        return repository.findByProductGroupAndCode(productGroup, code).map(this::toOverride);
    }

    @Override
    @Transactional
    public void upsert(String productGroup, String code, OverridePatch patch) {
        ProductGroupQuestionOverrideEntity entity = repository
                .findByProductGroupAndCode(productGroup, code)
                .orElseGet(() -> new ProductGroupQuestionOverrideEntity(
                        idGenerator.nextId(), productGroup, code));
        entity.apply(
                patch.label(), patch.helpText(), patch.required(),
                patch.displayOrder(), patch.active(), writeOptions(patch.options()));
        repository.save(entity);
    }

    private QuestionOverride toOverride(ProductGroupQuestionOverrideEntity entity) {
        return new QuestionOverride(
                entity.getCode(), entity.getLabel(), entity.getHelpText(), entity.getRequired(),
                entity.getDisplayOrder(), entity.isActive(), readOptions(entity.getOptionsJson()));
    }

    private List<Option> readOptions(String json) {
        if (json == null || json.isBlank()) {
            return null; // null → enum 기본 보기를 쓴다
        }
        try {
            return objectMapper.readValue(json, OPTION_LIST).stream()
                    .map(o -> new Option(o.value(), o.label()))
                    .toList();
        } catch (Exception e) {
            throw new BusinessException(
                    CommonErrorCode.INTERNAL_ERROR, "질문 보기 JSON 파싱 실패", java.util.Map.of(), e);
        }
    }

    private String writeOptions(List<Option> options) {
        if (options == null) {
            return null;
        }
        try {
            List<OptionJson> serializable = options.stream()
                    .map(o -> new OptionJson(o.value(), o.label()))
                    .toList();
            return objectMapper.writeValueAsString(serializable);
        } catch (Exception e) {
            throw new BusinessException(
                    CommonErrorCode.INTERNAL_ERROR, "질문 보기 JSON 직렬화 실패", java.util.Map.of(), e);
        }
    }

    private record OptionJson(String value, String label) {
    }
}
