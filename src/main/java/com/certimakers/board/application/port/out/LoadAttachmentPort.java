package com.certimakers.board.application.port.out;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * 첨부 파일 정보 조회. 파일 컨텍스트를 향한 아웃바운드 포트다.
 *
 * <p>파일 컨텍스트의 애그리거트({@code StoredFile})를 그대로 받지 않고 게시판이 필요한 필드만
 * 담은 {@link AttachmentInfo}로 좁혀 받는다. 다른 컨텍스트의 도메인 모델이 이쪽 인바운드 포트까지
 * 흘러 들어오면 두 컨텍스트가 한 덩어리가 되고, 파일 모델을 바꿀 때 게시판 API까지 흔들린다.
 */
public interface LoadAttachmentPort {

    /** 존재하는 첨부만 돌려준다. 지워진 파일 식별자는 결과에서 조용히 빠진다. */
    List<AttachmentInfo> findAll(Collection<UUID> fileIds);

    /**
     * @param downloadUrl 클라이언트가 그대로 쓸 수 있는 다운로드 경로
     */
    record AttachmentInfo(
            String fileId,
            String originalName,
            String contentType,
            long sizeInBytes,
            String downloadUrl) {
    }
}
