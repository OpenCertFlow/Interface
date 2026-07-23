package com.certimakers.document.domain.model;

import com.certimakers.common.domain.model.Guard;
import java.util.UUID;

/** 발급 문서 식별자. */
public record DocumentId(UUID value) {

    public DocumentId {
        Guard.notNull(value, "documentId");
    }

    public static DocumentId of(UUID value) {
        return new DocumentId(value);
    }
}
