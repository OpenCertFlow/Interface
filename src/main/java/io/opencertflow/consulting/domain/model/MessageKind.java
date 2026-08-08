package io.opencertflow.consulting.domain.model;

/**
 * 상담 메시지 종류. INFO_REQUEST·REPLY는 소공인에게 공개되고, NOTE는 컨설턴트 내부용이다.
 */
public enum MessageKind {

    INFO_REQUEST,
    REPLY,
    NOTE;

    public boolean isPublic() {
        return this != NOTE;
    }
}
