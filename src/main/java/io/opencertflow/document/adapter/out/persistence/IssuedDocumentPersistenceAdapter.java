package io.opencertflow.document.adapter.out.persistence;

import io.opencertflow.common.adapter.out.persistence.annotation.PersistenceAdapter;
import io.opencertflow.common.domain.error.BusinessException;
import io.opencertflow.common.domain.error.CommonErrorCode;
import io.opencertflow.document.application.port.out.IssuedDocumentRepositoryPort;
import io.opencertflow.document.domain.model.DocumentId;
import io.opencertflow.document.domain.model.DocumentTemplate;
import io.opencertflow.document.domain.model.FormValues;
import io.opencertflow.document.domain.model.IssuedDocument;
import io.opencertflow.document.domain.model.IssuerRef;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

/** {@link IssuedDocumentRepositoryPort}의 JPA 구현. 입력값은 JSON으로 직렬화해 담는다. */
@PersistenceAdapter
public class IssuedDocumentPersistenceAdapter implements IssuedDocumentRepositoryPort {

    private static final TypeReference<Map<String, String>> VALUES_TYPE = new TypeReference<>() {
    };

    private final IssuedDocumentJpaRepository repository;
    private final ObjectMapper objectMapper;

    public IssuedDocumentPersistenceAdapter(
            IssuedDocumentJpaRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public IssuedDocument save(IssuedDocument document) {
        repository.save(new IssuedDocumentEntity(
                document.id().value(),
                document.template().name(),
                serialize(document.values().values()),
                document.issuer().value(),
                document.fileId(),
                document.issuedAt()));
        return document;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<IssuedDocument> findById(DocumentId id) {
        return repository.findById(id.value()).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<IssuedDocument> findByIssuer(IssuerRef issuer, int page, int size) {
        return repository
                .findByIssuerIdOrderByIssuedAtDesc(issuer.value(), PageRequest.of(page, size))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    private String serialize(Map<String, String> values) {
        try {
            return objectMapper.writeValueAsString(values);
        } catch (JsonProcessingException e) {
            throw new BusinessException(
                    CommonErrorCode.INTERNAL_ERROR, "발급 내용을 저장하지 못했습니다.", Map.of(), e);
        }
    }

    private IssuedDocument toDomain(IssuedDocumentEntity entity) {
        DocumentTemplate template = DocumentTemplate.valueOf(entity.getTemplateCode());
        Map<String, String> values = deserialize(entity.getValuesJson());

        return IssuedDocument.reconstitute(
                DocumentId.of(entity.getId()),
                template,
                new FormValues(template, values),
                IssuerRef.of(entity.getIssuerId()),
                entity.getFileId(),
                entity.getIssuedAt());
    }

    private Map<String, String> deserialize(String json) {
        try {
            return objectMapper.readValue(json, VALUES_TYPE);
        } catch (JsonProcessingException e) {
            throw new BusinessException(
                    CommonErrorCode.INTERNAL_ERROR, "발급 내용을 읽지 못했습니다.", Map.of(), e);
        }
    }
}
