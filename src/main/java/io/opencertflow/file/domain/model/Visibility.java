package io.opencertflow.file.domain.model;

/** 파일 공개 범위. PUBLIC은 누구나, PRIVATE는 소유자·관리자만 다운로드할 수 있다. */
public enum Visibility {
    PUBLIC,
    PRIVATE
}
