package io.opencertflow.diagnosis.domain.model;

/**
 * 진단이 어느 부분에서 저하(degraded)되었는지 기록하는 가변 플래그.
 *
 * <p>둘 다 false면 온전한 COMPLETED, 하나라도 true면 COMPLETED_DEGRADED다. 플래그를 응답과 DB에
 * 모두 남겨, 시연 중 네트워크가 흔들려도 데모가 돌아가고 사후에 "왜 근거가 비었는가"를 답할 수
 * 있게 한다(03-diagnosis-flow.md).
 */
public class DegradedFlags {

    private boolean evidence;
    private boolean narration;

    public DegradedFlags() {
    }

    /** 저장된 값에서 되살릴 때 쓰는 팩토리(영속성 재구성 전용). */
    public static DegradedFlags of(boolean evidence, boolean narration) {
        DegradedFlags flags = new DegradedFlags();
        flags.evidence = evidence;
        flags.narration = narration;
        return flags;
    }

    /** RAG 근거 조회가 실패해 근거 없이 진행함. */
    public void markEvidence() {
        this.evidence = true;
    }

    /** LLM 문장화가 실패해 템플릿으로 대체함. */
    public void markNarration() {
        this.narration = true;
    }

    public boolean isEvidenceDegraded() {
        return evidence;
    }

    public boolean isNarrationDegraded() {
        return narration;
    }

    public boolean any() {
        return evidence || narration;
    }
}
