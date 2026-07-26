package com.certimakers.board.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.certimakers.board.application.port.in.PostUseCase.Requester;
import com.certimakers.board.application.port.in.PostUseCase.WritePostCommand;
import com.certimakers.board.application.port.out.CommentRepositoryPort;
import com.certimakers.board.application.port.out.LoadAttachmentPort;
import com.certimakers.board.application.port.out.PostRepositoryPort;
import com.certimakers.board.application.port.out.SyncAttachmentVisibilityPort;
import com.certimakers.board.domain.error.BoardErrorCode;
import com.certimakers.common.application.support.BlockingBridge;
import com.certimakers.common.domain.error.BusinessException;
import com.certimakers.common.domain.port.IdGenerator;
import com.certimakers.common.domain.port.TimeProvider;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

/**
 * 게시글 작성 시 첨부파일 소유권 검증(이슈 #27) 확인.
 *
 * <p>핵심은 <b>attachmentFileIds가 비어 있으면 소유권 검사 자체를 생략하고</b>, 하나라도 요청자
 * 소유가 아니면 저장 전에 거부되어 {@code postRepository.save()}가 호출되지 않는다는 것이다.
 */
class PostServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-10T12:00:00Z");
    private static final Requester REQUESTER = new Requester("1", false);

    private PostRepositoryPort postRepository;
    private CommentRepositoryPort commentRepository;
    private LoadAttachmentPort loadAttachmentPort;
    private SyncAttachmentVisibilityPort syncAttachmentVisibilityPort;
    private PostService service;

    @BeforeEach
    void setUp() {
        postRepository = Mockito.mock(PostRepositoryPort.class);
        commentRepository = Mockito.mock(CommentRepositoryPort.class);
        loadAttachmentPort = Mockito.mock(LoadAttachmentPort.class);
        syncAttachmentVisibilityPort = Mockito.mock(SyncAttachmentVisibilityPort.class);
        IdGenerator idGenerator = com.certimakers.support.TestIds::next;
        TimeProvider timeProvider = new TimeProvider() {
            @Override
            public Instant now() {
                return NOW;
            }

            @Override
            public ZoneId zone() {
                return ZoneId.of("UTC");
            }
        };

        when(postRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service = new PostService(
                postRepository, commentRepository, loadAttachmentPort, syncAttachmentVisibilityPort,
                new BlockingBridge(Schedulers.immediate()), idGenerator, timeProvider);
    }

    @Test
    @DisplayName("첨부파일이 없으면 소유권 검사를 생략하고 바로 저장한다")
    void 첨부파일_없으면_소유권_검사_생략() {
        WritePostCommand command =
                new WritePostCommand("FREE", REQUESTER, "제목", "본문", false, List.of());

        StepVerifier.create(service.write(command))
                .expectNextCount(1)
                .verifyComplete();

        verify(loadAttachmentPort, never()).findNotOwnedBy(any(), any());
        verify(postRepository).save(any());
    }

    @Test
    @DisplayName("첨부파일이 전부 본인 소유면 저장된다")
    void 본인_소유_파일만_첨부하면_저장된다() {
        when(loadAttachmentPort.findNotOwnedBy(any(), any())).thenReturn(List.of());
        WritePostCommand command =
                new WritePostCommand("FREE", REQUESTER, "제목", "본문", false, List.of("10"));

        StepVerifier.create(service.write(command))
                .expectNextCount(1)
                .verifyComplete();

        verify(postRepository).save(any());
    }

    @Test
    @DisplayName("남의 파일을 첨부하려 하면 거부하고 저장하지 않는다")
    void 남의_파일_첨부시_거부() {
        when(loadAttachmentPort.findNotOwnedBy(any(), any())).thenReturn(List.of(20L));
        WritePostCommand command =
                new WritePostCommand("FREE", REQUESTER, "제목", "본문", false, List.of("10", "20"));

        StepVerifier.create(service.write(command))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(BusinessException.class);
                    assertThat(((BusinessException) error).errorCode())
                            .isEqualTo(BoardErrorCode.ATTACHMENT_NOT_OWNED);
                })
                .verify();

        verify(postRepository, never()).save(any());
    }
}
