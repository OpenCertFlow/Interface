package io.opencertflow.file.adapter.in.web;

import io.opencertflow.file.domain.model.StoredFile;

/** 파일 API 응답 DTO. */
public final class FileResponses {

    private FileResponses() {
    }

    /**
     * 업로드 결과. <b>저장 키는 노출하지 않는다</b> — 내부 저장 구조를 드러낼 이유가 없고,
     * 클라이언트는 식별자만 있으면 다운로드할 수 있다.
     */
    public record Uploaded(
            String fileId,
            String originalName,
            String contentType,
            long sizeInBytes,
            String downloadUrl) {

        public static Uploaded from(StoredFile file) {
            String id = file.id().value().toString();
            return new Uploaded(
                    id,
                    file.originalName().value(),
                    file.contentType().value(),
                    file.sizeInBytes(),
                    "/api/v1/files/" + id);
        }
    }
}
