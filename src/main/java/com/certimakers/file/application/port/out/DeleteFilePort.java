package com.certimakers.file.application.port.out;

import com.certimakers.file.domain.model.FileId;

/** 파일 메타데이터 삭제. 블로킹이므로 호출자는 BlockingBridge로 감싼다. */
public interface DeleteFilePort {

    void deleteById(FileId id);
}
