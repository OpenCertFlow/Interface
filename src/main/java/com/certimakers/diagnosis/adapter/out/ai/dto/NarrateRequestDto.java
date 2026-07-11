package com.certimakers.diagnosis.adapter.out.ai.dto;

import com.certimakers.diagnosis.application.port.out.NarrationRequest;
import com.certimakers.diagnosis.domain.model.ChecklistItem;
import java.util.List;

/**
 * AI 워커 {@code POST /narrate} 요청 본문. 확정된 판정·점수·근거를 담아 넘긴다.
 *
 * <p>LLM은 이 값을 <b>바꾸지 못하고</b> 문장으로 옮기기만 한다. 누락 서류를 미리 뽑아 함께 보내
 * 워커가 "다음 행동"을 쉽게 구성하도록 돕는다.
 */
public record NarrateRequestDto(
        String productName,
        String productGroup,
        ScoreDto score,
        List<CandidateDto> candidates,
        List<DocumentDto> requiredDocuments,
        List<String> missingDocuments,
        List<ExpertItemDto> expertReviewItems,
        List<EvidenceDto> evidences) {

    public record ScoreDto(boolean applicable, int percentage) {
    }

    public record CandidateDto(String schemeCode, String certificationType) {
    }

    public record DocumentDto(String documentCode, String requirement, boolean held) {
    }

    public record ExpertItemDto(String question, String reason) {
    }

    public record EvidenceDto(String sectionType, String snippet, String sourceUrl) {
    }

    public static NarrateRequestDto from(NarrationRequest request) {
        List<String> missing = request.checklist().stream()
                .filter(ChecklistItem::isMissing)
                .map(item -> item.documentCode().value())
                .toList();

        return new NarrateRequestDto(
                request.profile().productName(),
                request.profile().productGroup().name(),
                new ScoreDto(request.score().applicable(), request.score().percentage()),
                request.candidates().stream()
                        .map(c -> new CandidateDto(c.schemeCode().value(), c.type().name()))
                        .toList(),
                request.checklist().stream()
                        .map(i -> new DocumentDto(i.documentCode().value(), i.requirement().name(), i.held()))
                        .toList(),
                missing,
                request.expertReviewItems().stream()
                        .map(e -> new ExpertItemDto(e.question(), e.reason().name()))
                        .toList(),
                request.evidences().stream()
                        .map(e -> new EvidenceDto(e.sectionType(), e.snippet(), e.sourceUrl().toString()))
                        .toList());
    }
}
