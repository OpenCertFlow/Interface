package io.opencertflow.file.application.port.out;

import io.opencertflow.file.domain.model.StoredFile;

/** 파일 메타데이터 저장. 블로킹이므로 호출자는 BlockingBridge로 감싼다. */
public interface SaveFilePort {

    StoredFile save(StoredFile file);
}
