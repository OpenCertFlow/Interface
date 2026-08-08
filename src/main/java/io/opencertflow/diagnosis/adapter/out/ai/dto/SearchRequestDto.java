package io.opencertflow.diagnosis.adapter.out.ai.dto;

import io.opencertflow.diagnosis.application.port.out.EvidenceQuery;
import io.opencertflow.diagnosis.domain.model.CertificationType;
import io.opencertflow.diagnosis.domain.model.SchemeCode;
import java.util.List;

/**
 * AI 워커 {@code POST /search} 요청 본문. 계약은 docs/api/ai-worker.md 참조.
 */
public record SearchRequestDto(
        String productGroup,
        List<String> schemeCodes,
        List<String> certificationTypes,
        List<String> sections) {

    public static SearchRequestDto from(EvidenceQuery query) {
        return new SearchRequestDto(
                query.productGroup().name(),
                query.schemeCodes().stream().map(SchemeCode::value).sorted().toList(),
                query.certificationTypes().stream().map(CertificationType::name).sorted().toList(),
                query.sections());
    }
}
