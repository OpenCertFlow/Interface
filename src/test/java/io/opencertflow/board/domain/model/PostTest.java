package io.opencertflow.board.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.opencertflow.board.domain.error.BoardErrorCode;
import io.opencertflow.common.domain.error.BusinessException;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * 게시글 애그리거트가 게시판별 정책을 실제로 강제하는지 검증한다.
 *
 * <p>이 테스트가 이 프로젝트에서 중요한 이유는, "게시판 타입별 기능 분리"가 코드에서 실현되는
 * 지점이 바로 이 애그리거트이기 때문이다. 규칙이 새면 공지에 아무나 글을 쓰고 남의 글을 지운다.
 */
class PostTest {

    private static final Instant NOW = Instant.parse("2026-08-10T12:00:00Z");

    private static AuthorRef author() {
        return AuthorRef.of(io.opencertflow.support.TestIds.next());
    }

    private static PostContent content() {
        return PostContent.of("제목", "본문");
    }

    private static Post write(BoardType type, AuthorRef author, boolean isAdmin) {
        return Post.write(
                PostId.of(io.opencertflow.support.TestIds.next()), type, author, isAdmin,
                content(), false, List.of(), NOW);
    }

    @Nested
    @DisplayName("작성 권한")
    class WritePermission {

        @ParameterizedTest
        @EnumSource(value = BoardType.class, names = {"NOTICE", "ARCHIVE"})
        @DisplayName("관리자 전용 게시판에는 일반 회원이 글을 쓸 수 없다")
        void 관리자_전용_게시판은_일반회원을_거부한다(BoardType adminOnly) {
            assertThatThrownBy(() -> write(adminOnly, author(), false))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(error -> assertThat(((BusinessException) error).errorCode())
                            .isEqualTo(BoardErrorCode.ADMIN_ONLY_BOARD));
        }

        @ParameterizedTest
        @EnumSource(value = BoardType.class, names = {"NOTICE", "ARCHIVE"})
        @DisplayName("관리자는 관리자 전용 게시판에 글을 쓸 수 있다")
        void 관리자는_관리자_전용_게시판에_쓸_수_있다(BoardType adminOnly) {
            assertThatCode(() -> write(adminOnly, author(), true)).doesNotThrowAnyException();
        }

        @ParameterizedTest
        @EnumSource(value = BoardType.class, names = {"FREE", "QNA"})
        @DisplayName("일반 게시판에는 회원 누구나 글을 쓸 수 있다")
        void 일반_게시판은_회원_누구나_쓸_수_있다(BoardType open) {
            assertThatCode(() -> write(open, author(), false)).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("비밀글 정책")
    class SecretPolicy {

        @Test
        @DisplayName("질문게시판만 비밀글을 허용한다")
        void 질문게시판만_비밀글을_허용한다() {
            assertThatCode(() -> Post.write(
                    PostId.of(io.opencertflow.support.TestIds.next()), BoardType.QNA, author(), false,
                    content(), true, List.of(), NOW))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("자유게시판에는 비밀글을 쓸 수 없다")
        void 자유게시판은_비밀글을_거부한다() {
            assertThatThrownBy(() -> Post.write(
                    PostId.of(io.opencertflow.support.TestIds.next()), BoardType.FREE, author(), false,
                    content(), true, List.of(), NOW))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(error -> assertThat(((BusinessException) error).errorCode())
                            .isEqualTo(BoardErrorCode.SECRET_POST_NOT_ALLOWED));
        }

        @Test
        @DisplayName("비밀글은 작성자와 관리자만 볼 수 있다")
        void 비밀글은_작성자와_관리자만_본다() {
            AuthorRef owner = author();
            AuthorRef stranger = author();
            Post secret = Post.write(
                    PostId.of(io.opencertflow.support.TestIds.next()), BoardType.QNA, owner, false,
                    content(), true, List.of(), NOW);

            assertThatCode(() -> secret.requireReadableBy(owner, false)).doesNotThrowAnyException();
            assertThatCode(() -> secret.requireReadableBy(stranger, true)).doesNotThrowAnyException();
            assertThatThrownBy(() -> secret.requireReadableBy(stranger, false))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("비로그인 사용자는 비밀글을 볼 수 없다")
        void 비로그인은_비밀글을_볼_수_없다() {
            Post secret = Post.write(
                    PostId.of(io.opencertflow.support.TestIds.next()), BoardType.QNA, author(), false,
                    content(), true, List.of(), NOW);

            assertThatThrownBy(() -> secret.requireReadableBy(null, false))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("공개글은 비로그인도 볼 수 있다")
        void 공개글은_누구나_본다() {
            Post open = write(BoardType.FREE, author(), false);

            assertThatCode(() -> open.requireReadableBy(null, false)).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("댓글 허용 정책")
    class CommentPolicy {

        @ParameterizedTest
        @EnumSource(value = BoardType.class, names = {"NOTICE", "ARCHIVE"})
        @DisplayName("공지·자료실에는 댓글을 달 수 없다")
        void 공지와_자료실은_댓글을_거부한다(BoardType noComments) {
            Post post = write(noComments, author(), true);

            assertThatThrownBy(post::requireCommentable)
                    .isInstanceOf(BusinessException.class)
                    .satisfies(error -> assertThat(((BusinessException) error).errorCode())
                            .isEqualTo(BoardErrorCode.COMMENTS_NOT_ALLOWED));
        }

        @ParameterizedTest
        @EnumSource(value = BoardType.class, names = {"FREE", "QNA"})
        @DisplayName("자유·질문게시판에는 댓글을 달 수 있다")
        void 자유와_질문게시판은_댓글을_허용한다(BoardType withComments) {
            Post post = write(withComments, author(), false);

            assertThatCode(post::requireCommentable).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("수정·삭제 권한")
    class EditPermission {

        @Test
        @DisplayName("작성자 본인은 수정할 수 있다")
        void 작성자는_수정할_수_있다() {
            AuthorRef owner = author();
            Post post = write(BoardType.FREE, owner, false);

            assertThatCode(() -> post.requireEditableBy(owner, false)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("남의 글은 수정할 수 없다")
        void 남의_글은_수정할_수_없다() {
            Post post = write(BoardType.FREE, author(), false);

            assertThatThrownBy(() -> post.requireEditableBy(author(), false))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(error -> assertThat(((BusinessException) error).errorCode())
                            .isEqualTo(BoardErrorCode.NOT_POST_AUTHOR));
        }

        @Test
        @DisplayName("관리자는 남의 글도 수정·삭제할 수 있다")
        void 관리자는_남의_글도_다룰_수_있다() {
            Post post = write(BoardType.FREE, author(), false);

            assertThatCode(() -> post.requireEditableBy(author(), true)).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("첨부 정책")
    class AttachmentPolicy {

        @Test
        @DisplayName("중복 첨부는 하나로 접는다")
        void 중복_첨부는_하나로_접는다() {
            Long duplicated = io.opencertflow.support.TestIds.next();
            Post post = Post.write(
                    PostId.of(io.opencertflow.support.TestIds.next()), BoardType.FREE, author(), false,
                    content(), false, List.of(duplicated, duplicated), NOW);

            assertThat(post.attachmentFileIds()).containsExactly(duplicated);
        }

        @Test
        @DisplayName("첨부 개수 상한을 넘으면 거부한다")
        void 첨부_개수_상한을_강제한다() {
            List<Long> tooMany = java.util.stream.Stream.generate(io.opencertflow.support.TestIds::next)
                    .limit(6)
                    .toList();

            assertThatThrownBy(() -> Post.write(
                    PostId.of(io.opencertflow.support.TestIds.next()), BoardType.FREE, author(), false,
                    content(), false, tooMany, NOW))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(error -> assertThat(((BusinessException) error).errorCode())
                            .isEqualTo(BoardErrorCode.TOO_MANY_ATTACHMENTS));
        }
    }

    @Test
    @DisplayName("조회수는 상세 조회에서만 올라간다")
    void 조회수를_올릴_수_있다() {
        Post post = write(BoardType.FREE, author(), false);

        post.increaseViewCount();
        post.increaseViewCount();

        assertThat(post.viewCount()).isEqualTo(2);
    }
}
