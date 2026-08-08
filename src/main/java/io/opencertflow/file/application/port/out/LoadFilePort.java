package io.opencertflow.file.application.port.out;

import io.opencertflow.file.domain.model.FileId;
import io.opencertflow.file.domain.model.StoredFile;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** 파일 메타데이터 조회. 블로킹이므로 호출자는 BlockingBridge로 감싼다. */
public interface LoadFilePort {

    Optional<StoredFile> findById(FileId id);

    /** 게시글 첨부 목록처럼 여러 파일을 한 번에 읽을 때 쓴다. N+1 조회를 피하기 위함. */
    List<StoredFile> findAllByIds(Collection<FileId> ids);
}
