package io.opencertflow.diagnosis.application.service;

import io.opencertflow.common.application.annotation.UseCase;
import io.opencertflow.common.domain.error.BusinessException;
import io.opencertflow.diagnosis.application.port.in.VerifyRagUseCase;
import io.opencertflow.diagnosis.application.port.out.EvidenceQuery;
import io.opencertflow.diagnosis.application.port.out.SearchEvidencePort;
import io.opencertflow.diagnosis.domain.model.CertificationType;
import io.opencertflow.diagnosis.domain.model.ProductGroup;
import io.opencertflow.diagnosis.domain.model.SchemeCode;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

/**
 * RAG 품질 검증. 검색 실패는 degraded로 표시해 돌려준다 — 검증 자체가 예외로 죽지 않게 한다
 * (RAG 워커가 꺼져 있어도 "지금 근거가 안 붙는다"를 관리자가 확인할 수 있어야 한다).
 */
@UseCase
public class RagQualityService implements VerifyRagUseCase {

    private static final Logger log = LoggerFactory.getLogger(RagQualityService.class);
    private static final List<String> DEFAULT_SECTIONS = List.of("DOCUMENTS", "LABELING");

    private final SearchEvidencePort searchEvidencePort;

    public RagQualityService(SearchEvidencePort searchEvidencePort) {
        this.searchEvidencePort = searchEvidencePort;
    }

    @Override
    public Mono<RagCheckResult> check(RagCheckCommand command) {
        EvidenceQuery query = toQuery(command);
        return searchEvidencePort.search(query)
                .map(evidences -> new RagCheckResult(
                        evidences.size(), false,
                        evidences.stream()
                                .map(e -> new EvidenceView(
                                        e.sourceDocumentId(), e.sectionType(), e.snippet(),
                                        e.sourceUrl().toString(), e.relevance()))
                                .toList()))
                .onErrorResume(error -> {
                    log.warn("RAG 검증 검색 실패 — degraded로 보고한다. cause={}", error.toString());
                    return Mono.just(new RagCheckResult(0, true, List.of()));
                });
    }

    private EvidenceQuery toQuery(RagCheckCommand command) {
        ProductGroup group = parseProductGroup(command.productGroup());
        Set<SchemeCode> schemes = command.schemeCodes() == null ? Set.of()
                : command.schemeCodes().stream().map(SchemeCode::of)
                        .collect(Collectors.toUnmodifiableSet());
        Set<CertificationType> types = command.certificationTypes() == null ? Set.of()
                : command.certificationTypes().stream().map(this::parseCertificationType)
                        .collect(Collectors.toUnmodifiableSet());
        List<String> sections = command.sections() == null || command.sections().isEmpty()
                ? DEFAULT_SECTIONS : command.sections();
        return new EvidenceQuery(group, schemes, types, sections);
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

    private CertificationType parseCertificationType(String raw) {
        try {
            return CertificationType.valueOf(raw.strip().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw BusinessException.invalid("인증 유형 값이 올바르지 않습니다: " + raw);
        }
    }
}
