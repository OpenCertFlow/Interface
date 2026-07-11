package com.certimakers.diagnosis.domain.model;

import com.certimakers.common.domain.model.Guard;
import java.net.URI;

/**
 * 공식 문서에서 검색된 근거 문단. RAG 워커가 찾아 준다.
 *
 * <p>불변식 6(04-domain-model.md): {@code sourceUrl} 없이 존재할 수 없다. 출처 없는 근거는 근거가
 * 아니다. RAG가 임계 유사도를 넘는 문단을 못 찾으면 억지로 Evidence를 만들지 않고, 대신
 * {@link ExpertReviewItem}(NO_EVIDENCE)을 만든다(05-data-model.md의 score_threshold).
 *
 * @param sourceDocumentId  근거가 나온 공식 문서 식별자
 * @param sectionType       문서 내 섹션 (SCHEME · SCOPE · DOCUMENTS · LABELING · EXCEPTION)
 * @param snippet           근거 문단 발췌
 * @param sourceUrl         원문 링크 (필수)
 * @param relevance         검색 유사도 점수
 */
public record Evidence(
        String sourceDocumentId,
        String sectionType,
        String snippet,
        URI sourceUrl,
        double relevance) {

    public Evidence {
        Guard.hasText(sourceDocumentId, "sourceDocumentId");
        Guard.hasText(sectionType, "sectionType");
        Guard.hasText(snippet, "snippet");
        Guard.notNull(sourceUrl, "sourceUrl"); // 불변식 6
    }
}
