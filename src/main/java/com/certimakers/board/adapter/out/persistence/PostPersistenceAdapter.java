package com.certimakers.board.adapter.out.persistence;

import com.certimakers.board.application.port.out.PostRepositoryPort;
import com.certimakers.board.domain.model.AuthorRef;
import com.certimakers.board.domain.model.BoardType;
import com.certimakers.board.domain.model.Post;
import com.certimakers.board.domain.model.PostContent;
import com.certimakers.board.domain.model.PostId;
import com.certimakers.common.adapter.out.persistence.annotation.PersistenceAdapter;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

/** {@link PostRepositoryPort}의 JPA 구현. 메서드는 블로킹이다. */
@PersistenceAdapter
public class PostPersistenceAdapter implements PostRepositoryPort {

    private static final String ATTACHMENT_DELIMITER = ",";

    private final PostJpaRepository repository;

    public PostPersistenceAdapter(PostJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public Post save(Post post) {
        repository.save(toEntity(post));
        return post;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Post> findById(PostId id) {
        return repository.findById(id.value()).map(PostPersistenceAdapter::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Post> findByBoardType(BoardType boardType, int page, int size) {
        return repository
                .findByBoardTypeOrderByCreatedAtDesc(boardType.name(), PageRequest.of(page, size))
                .stream()
                .map(PostPersistenceAdapter::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long countByBoardType(BoardType boardType) {
        return repository.countByBoardType(boardType.name());
    }

    @Override
    @Transactional
    public void deleteById(PostId id) {
        repository.deleteById(id.value());
    }

    private static PostEntity toEntity(Post post) {
        String attachments = post.attachmentFileIds().isEmpty()
                ? null
                : post.attachmentFileIds().stream()
                        .map(UUID::toString)
                        .collect(Collectors.joining(ATTACHMENT_DELIMITER));

        return new PostEntity(
                post.id().value(),
                post.boardType().name(),
                post.author().value(),
                post.content().title(),
                post.content().body(),
                post.isSecret(),
                attachments,
                post.viewCount(),
                post.createdAt(),
                post.updatedAt());
    }

    private static Post toDomain(PostEntity entity) {
        return Post.reconstitute(
                PostId.of(entity.getId()),
                BoardType.valueOf(entity.getBoardType()),
                AuthorRef.of(entity.getAuthorId()),
                PostContent.of(entity.getTitle(), entity.getBody()),
                entity.isSecret(),
                parseAttachments(entity.getAttachmentIds()),
                entity.getViewCount(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    private static List<UUID> parseAttachments(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return Arrays.stream(raw.split(ATTACHMENT_DELIMITER))
                .map(String::strip)
                .filter(value -> !value.isEmpty())
                .map(UUID::fromString)
                .toList();
    }
}
