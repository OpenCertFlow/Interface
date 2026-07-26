package com.certimakers.board.application.port.out;

import java.util.Collection;

/**
 * 글의 비밀글 여부에 맞춰 첨부파일의 공개 범위를 file 컨텍스트에 동기화한다.
 *
 * <p>파일 업로드 시점엔 그 파일이 비밀글에 붙을지 아직 모른다. 그래서 공개 범위는 업로드가
 * 아니라 글쓰기·수정 시점에 다시 정해져야 한다.
 */
public interface SyncAttachmentVisibilityPort {

    void syncVisibility(Collection<Long> fileIds, Long ownerId, boolean secret);
}
