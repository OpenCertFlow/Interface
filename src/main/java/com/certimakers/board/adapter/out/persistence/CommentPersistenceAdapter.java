package com.certimakers.board.adapter.out.persistence;

import com.certimakers.board.application.port.out.CommentRepositoryPort;
import com.certimakers.board.domain.model.AuthorRef;
import com.certimakers.board.domain.model.Comment;
import com.certimakers.board.domain.model.CommentId;
import com.certimakers.board.domain.model.PostId;
import com.certimakers.common.adapter.out.persistence.annotation.PersistenceAdapter;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.transaction.annotation.Transactional;

/** {@link CommentRepositoryPort}의 JPA 구현. 메서드는 블로킹이다. */
@PersistenceAdapter
public class CommentPersistenceAdapter implements CommentRepositoryPort {

    private final CommentJpaRepository repository;

    public CommentPersistenceAdapter(CommentJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public Comment save(Comment comment) {
        repository.save(toEntity(comment));
        return comment;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Comment> findById(CommentId id) {
        return repository.findById(id.value()).map(CommentPersistenceAdapter::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Comment> findByPostId(PostId postId) {
        return repository.findByPostIdOrderByCreatedAtAsc(postId.value()).stream()
                .map(CommentPersistenceAdapter::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, Integer> countByPostIds(Collection<PostId> postIds) {
        if (postIds.isEmpty()) {
            return Map.of();
        }
        List<Long> raw = postIds.stream().map(PostId::value).toList();

        Map<Long, Integer> counts = new HashMap<>();
        for (Object[] row : repository.countGroupedByPostId(raw)) {
            counts.put((Long) row[0], ((Number) row[1]).intValue());
        }
        return counts;
    }

    @Override
    @Transactional
    public void deleteById(CommentId id) {
        repository.deleteById(id.value());
    }

    @Override
    @Transactional
    public void deleteByPostId(PostId postId) {
        repository.deleteByPostId(postId.value());
    }

    private static CommentEntity toEntity(Comment comment) {
        return new CommentEntity(
                comment.id().value(),
                comment.postId().value(),
                comment.author().value(),
                comment.body(),
                comment.createdAt(),
                comment.updatedAt());
    }

    private static Comment toDomain(CommentEntity entity) {
        return Comment.reconstitute(
                CommentId.of(entity.getId()),
                PostId.of(entity.getPostId()),
                AuthorRef.of(entity.getAuthorId()),
                entity.getBody(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}
