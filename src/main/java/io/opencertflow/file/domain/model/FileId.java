package io.opencertflow.file.domain.model;

import io.opencertflow.common.domain.model.Guard;

/** 파일 식별자. 값은 전역 시퀀스에서 나오며 {@code IdGenerator} 포트가 생성한다. */
public record FileId(Long value) {

    public FileId {
        Guard.notNull(value, "fileId");
    }

    public static FileId of(Long value) {
        return new FileId(value);
    }
}
