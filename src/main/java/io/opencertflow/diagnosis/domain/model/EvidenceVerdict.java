package io.opencertflow.diagnosis.domain.model;

/**
 * 컨설턴트가 근거에 내린 판단.
 *
 * <p>'쓸모없음' 하나로 뭉치지 않는 이유는 <b>고칠 방법이 다르기</b> 때문이다. 낡은 문서는 원문을
 * 다시 받아야 하고, 다른 제품 문서는 색인 태그를 고쳐야 하며, 무관한 문서는 청킹이나 임계값
 * 문제일 수 있다. 뭉쳐 놓으면 재검토 큐를 받아도 무엇부터 해야 할지 알 수 없다.
 */
public enum EvidenceVerdict {

    /** 상담에 실제로 도움이 됐다. 좋은 근거의 표본이 된다. */
    USEFUL,

    /** 이 제품 상황과 관련이 없다. 검색 품질 문제일 수 있다. */
    IRRELEVANT,

    /** 내용이 낡았다. 원문 재확인이 필요하다. */
    OUTDATED,

    /** 다른 제품군·제도의 문서다. 색인 태그가 잘못됐다는 신호다. */
    WRONG_PRODUCT;

    /** 색인 재검토가 필요한 판단인지. USEFUL만 아니면 참이다. */
    public boolean needsReview() {
        return this != USEFUL;
    }
}
