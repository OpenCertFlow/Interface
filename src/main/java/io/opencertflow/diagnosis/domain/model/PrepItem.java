package io.opencertflow.diagnosis.domain.model;

import io.opencertflow.common.domain.model.Guard;

/**
 * 준비목록의 한 줄. 서류 하나와 확보 여부다.
 *
 * <p>확보 여부가 바뀌므로 {@code record}가 아니라 가변 클래스다({@code DegradedFlags}와 같은 이유).
 * 애그리거트 루트인 {@link PrepPlan}을 통해서만 바뀐다 — 목록을 밖에서 직접 만지지 못하게
 * {@code PrepPlan}이 불변 뷰로 노출한다.
 */
public class PrepItem {

    private final DocumentCode documentCode;
    private boolean done;

    private PrepItem(DocumentCode documentCode, boolean done) {
        this.documentCode = Guard.notNull(documentCode, "documentCode");
        this.done = done;
    }

    /** 아직 확보하지 않은 항목으로 시작한다. */
    public static PrepItem of(DocumentCode documentCode) {
        return new PrepItem(documentCode, false);
    }

    /** 저장된 상태에서 되살린다. 영속성 재구성 전용이다. */
    public static PrepItem reconstitute(DocumentCode documentCode, boolean done) {
        return new PrepItem(documentCode, done);
    }

    /** 확보 여부를 바꾼다. {@link PrepPlan}만 호출한다(패키지 밖에서는 접근 불가). */
    void markDone(boolean done) {
        this.done = done;
    }

    public DocumentCode documentCode() {
        return documentCode;
    }

    public boolean done() {
        return done;
    }
}
