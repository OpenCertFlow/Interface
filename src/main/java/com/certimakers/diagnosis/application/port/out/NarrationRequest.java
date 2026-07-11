package com.certimakers.diagnosis.application.port.out;

import com.certimakers.common.domain.model.Guard;
import com.certimakers.diagnosis.domain.model.CertificationCandidate;
import com.certimakers.diagnosis.domain.model.ChecklistItem;
import com.certimakers.diagnosis.domain.model.Diagnosis;
import com.certimakers.diagnosis.domain.model.Evidence;
import com.certimakers.diagnosis.domain.model.ExpertReviewItem;
import com.certimakers.diagnosis.domain.model.ProductProfile;
import com.certimakers.diagnosis.domain.model.ReadinessScore;
import java.util.List;

/**
 * LLM 문장화 요청. 확정된 판정·점수·근거를 담아 넘긴다.
 *
 * <p>LLM은 이 안의 어떤 값도 바꾸지 않는다. 이미 확정된 결과를 사용자가 읽을 문장으로 옮길 뿐이다.
 * 근거({@code evidences})가 비어 있으면 프롬프트가 그 사실을 알고 단정을 피하도록 구성된다(ADR-0003).
 */
public record NarrationRequest(
        ProductProfile profile,
        ReadinessScore score,
        List<CertificationCandidate> candidates,
        List<ChecklistItem> checklist,
        List<ExpertReviewItem> expertReviewItems,
        List<Evidence> evidences) {

    public NarrationRequest {
        Guard.notNull(profile, "profile");
        Guard.notNull(score, "score");
        candidates = List.copyOf(Guard.notNull(candidates, "candidates"));
        checklist = List.copyOf(Guard.notNull(checklist, "checklist"));
        expertReviewItems = List.copyOf(Guard.notNull(expertReviewItems, "expertReviewItems"));
        evidences = List.copyOf(Guard.notNull(evidences, "evidences"));
    }

    /** 근거까지 첨부된 진단에서 문장화 요청을 만든다. */
    public static NarrationRequest from(Diagnosis diagnosis) {
        return new NarrationRequest(
                diagnosis.profile(),
                diagnosis.score(),
                diagnosis.candidates(),
                diagnosis.checklist(),
                diagnosis.expertReviewItems(),
                diagnosis.evidences());
    }
}
