package io.opencertflow.diagnosis.adapter.out.ai.dto;

import io.opencertflow.diagnosis.domain.model.Evidence;
import java.net.URI;
import java.util.List;

/**
 * AI 워커 {@code POST /search} 응답 본문.
 *
 * <p>sourceUrl이 없는 근거는 도메인이 거부한다(불변식 6). 워커가 임계 유사도 미달 근거를 걸러
 * 보내므로, 여기 오는 것은 출처가 있는 근거뿐이라고 가정한다.
 */
public record SearchResponseDto(List<EvidenceItem> evidences) {

    public record EvidenceItem(
            String sourceDocumentId,
            String sectionType,
            String snippet,
            String sourceUrl,
            double relevance) {

        public Evidence toDomain() {
            return new Evidence(sourceDocumentId, sectionType, snippet, URI.create(sourceUrl), relevance);
        }
    }

    public List<Evidence> toDomain() {
        return evidences == null ? List.of() : evidences.stream().map(EvidenceItem::toDomain).toList();
    }
}
