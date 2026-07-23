package com.certimakers.file.domain.model;

import com.certimakers.common.domain.model.Guard;
import java.util.UUID;

/** 파일 식별자. 값은 UUIDv7(시간 정렬)이며 {@code IdGenerator} 포트가 생성한다. */
public record FileId(UUID value) {

    public FileId {
        Guard.notNull(value, "fileId");
    }

    public static FileId of(UUID value) {
        return new FileId(value);
    }
}
