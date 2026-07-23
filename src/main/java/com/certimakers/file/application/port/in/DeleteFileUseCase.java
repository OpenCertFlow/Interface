package com.certimakers.file.application.port.in;

import reactor.core.publisher.Mono;

/** 파일 삭제. 업로더 본인 또는 관리자만 가능하다. */
public interface DeleteFileUseCase {

    Mono<Void> delete(DeleteCommand command);

    record DeleteCommand(String fileId, String requesterId, boolean requesterIsAdmin) {
    }
}
