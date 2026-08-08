package io.opencertflow.board.adapter.in.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

/** 게시판 API 요청 DTO. */
public final class BoardRequests {

    private BoardRequests() {
    }

    public record WritePost(
            @NotBlank(message = "제목을 입력해 주세요.")
            @Size(max = 200, message = "제목은 200자 이하로 입력해 주세요.")
            String title,

            @NotBlank(message = "내용을 입력해 주세요.")
            String body,

            /** 비밀글 여부. 허용하지 않는 게시판이면 도메인이 거부한다. */
            boolean secret,

            /** 첨부 파일 식별자. 파일을 먼저 업로드해 받은 값을 넣는다. */
            List<String> attachmentFileIds) {

        public WritePost {
            attachmentFileIds = attachmentFileIds == null ? List.of() : List.copyOf(attachmentFileIds);
        }
    }

    public record WriteComment(
            @NotBlank(message = "댓글 내용을 입력해 주세요.")
            @Size(max = 2000, message = "댓글은 2000자 이하로 입력해 주세요.")
            String body) {
    }
}
