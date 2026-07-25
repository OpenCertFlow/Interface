package com.certimakers.document.domain.model;

import com.certimakers.common.domain.model.Guard;

/** 발급 문서 식별자. */
public record DocumentId(Long value) {

    public DocumentId {
        Guard.notNull(value, "documentId");
    }

    public static DocumentId of(Long value) {
        return new DocumentId(value);
    }
}
