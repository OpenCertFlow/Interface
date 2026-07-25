package com.certimakers.board.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * {@code board_post} 테이블 매핑.
 *
 * <p>첨부 파일 식별자는 별도 테이블({@code board_post_attachment})에 두지 않고 쉼표로 이어 붙인
 * 문자열로 담는다. 첨부가 최대 5개로 제한되고 항상 글과 함께 읽히므로, 조인 테이블을 만드는 것은
 * 이 규모에서 과하다. 개수 제한이 커지면 그때 테이블로 분리한다.
 */
@Entity
@Table(name = "board_post")
public class PostEntity {

    @Id
    private Long id;

    @Column(name = "board_type", nullable = false)
    private String boardType;

    @Column(name = "author_id", nullable = false)
    private Long authorId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    @Column(nullable = false)
    private boolean secret;

    @Column(name = "attachment_ids")
    private String attachmentIds; // 쉼표로 이어 붙인 Long 목록. 없으면 null

    @Column(name = "view_count", nullable = false)
    private long viewCount;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PostEntity() {
    }

    public PostEntity(
            Long id, String boardType, Long authorId, String title, String body, boolean secret,
            String attachmentIds, long viewCount, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.boardType = boardType;
        this.authorId = authorId;
        this.title = title;
        this.body = body;
        this.secret = secret;
        this.attachmentIds = attachmentIds;
        this.viewCount = viewCount;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public String getBoardType() {
        return boardType;
    }

    public Long getAuthorId() {
        return authorId;
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }

    public boolean isSecret() {
        return secret;
    }

    public String getAttachmentIds() {
        return attachmentIds;
    }

    public long getViewCount() {
        return viewCount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
