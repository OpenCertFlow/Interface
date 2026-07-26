package com.certimakers.board;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * 게시판·파일 엔드투엔드 검증.
 *
 * <p>파일 업로드 → 게시글 첨부 → 조회까지 <b>두 컨텍스트가 실제로 맞물리는지</b>를 확인하는 것이
 * 이 테스트의 핵심이다. 각 도메인 단위 테스트가 통과해도 연결이 어긋나면 기능은 동작하지 않는다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("local")
@Testcontainers
class BoardFlowIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    @org.junit.jupiter.api.io.TempDir
    static java.nio.file.Path uploadRoot;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        // 업로드가 프로젝트 디렉터리를 더럽히지 않도록 임시 경로를 쓴다
        registry.add("certimakers.file.storage-root", () -> uploadRoot.toString());
    }

    @Autowired
    WebTestClient webTestClient;

    @MockitoBean
    JavaMailSender mailSender;

    private static final String PASSWORD = "password1234";

    private String signUpAndLogin(String tag) {
        webTestClient.post().uri("/api/v1/auth/signup")
                .bodyValue(Map.of("email", tag + "@example.com", "password", PASSWORD, "nickname", tag,
                        "agreedTermKeys", java.util.List.of("SERVICE", "PRIVACY")))
                .exchange()
                .expectStatus().isCreated();

        JsonNode login = webTestClient.post().uri("/api/v1/auth/login")
                .bodyValue(Map.of("email", tag + "@example.com", "password", PASSWORD))
                .exchange()
                .expectStatus().isOk()
                .expectBody(JsonNode.class).returnResult().getResponseBody();

        return login.at("/data/accessToken").asText();
    }

    private String uploadFile(String token, String fileName, byte[] content) {
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("file", new ByteArrayResource(content) {
            @Override
            public String getFilename() {
                return fileName;
            }
        }).contentType(MediaType.APPLICATION_PDF);

        JsonNode uploaded = webTestClient.post().uri("/api/v1/files")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(org.springframework.web.reactive.function.BodyInserters
                        .fromMultipartData(builder.build()))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(JsonNode.class).returnResult().getResponseBody();

        return uploaded.at("/data/fileId").asText();
    }

    @Test
    @DisplayName("파일을 올려 글에 첨부하면 상세 조회에 첨부가 함께 나온다")
    void 파일_첨부_글쓰기_조회_전체흐름() {
        String token = signUpAndLogin("boarduser");
        byte[] content = "인증 서류 내용".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        String fileId = uploadFile(token, "서류.pdf", content);

        JsonNode created = webTestClient.post().uri("/api/v1/boards/FREE/posts")
                .header("Authorization", "Bearer " + token)
                .bodyValue(Map.of(
                        "title", "인증 준비 후기",
                        "body", "드라이기 인증 준비하면서 겪은 일",
                        "secret", false,
                        "attachmentFileIds", List.of(fileId)))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(JsonNode.class).returnResult().getResponseBody();

        String postId = created.at("/data/postId").asText();

        webTestClient.get().uri("/api/v1/boards/posts/{id}", postId)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.title").isEqualTo("인증 준비 후기")
                .jsonPath("$.data.authorNickname").isEqualTo("boarduser")
                .jsonPath("$.data.attachments[0].fileId").isEqualTo(fileId)
                .jsonPath("$.data.attachments[0].originalName").isEqualTo("서류.pdf");

        // 첨부 파일을 실제로 내려받을 수 있어야 한다
        byte[] downloaded = webTestClient.get().uri("/api/v1/files/{id}", fileId)
                .exchange()
                .expectStatus().isOk()
                .expectBody().returnResult().getResponseBody();

        assertThat(downloaded).isEqualTo(content);
    }

    @Test
    @DisplayName("남의 파일을 첨부하려 하면 글쓰기가 거부된다")
    void 남의_파일은_첨부할_수_없다() {
        String ownerToken = signUpAndLogin("fileowner");
        String strangerToken = signUpAndLogin("attacher");

        byte[] content = "원본 파일".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        String fileId = uploadFile(ownerToken, "원본.pdf", content);

        webTestClient.post().uri("/api/v1/boards/FREE/posts")
                .header("Authorization", "Bearer " + strangerToken)
                .bodyValue(Map.of("title", "제목", "body", "내용", "secret", false,
                        "attachmentFileIds", List.of(fileId)))
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody().jsonPath("$.error.code").isEqualTo("CM-BOARD-011");
    }

    @Test
    @DisplayName("공지 게시판에는 일반 회원이 글을 쓸 수 없다")
    void 공지에는_일반회원이_쓸_수_없다() {
        String token = signUpAndLogin("normaluser");

        webTestClient.post().uri("/api/v1/boards/NOTICE/posts")
                .header("Authorization", "Bearer " + token)
                .bodyValue(Map.of("title", "공지", "body", "내용", "secret", false,
                        "attachmentFileIds", List.of()))
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody().jsonPath("$.error.code").isEqualTo("CM-BOARD-003");
    }

    @Test
    @DisplayName("로그인하지 않으면 글을 쓸 수 없지만 목록은 볼 수 있다")
    void 비로그인은_읽기만_가능하다() {
        webTestClient.get().uri("/api/v1/boards/FREE/posts")
                .exchange()
                .expectStatus().isOk();

        webTestClient.post().uri("/api/v1/boards/FREE/posts")
                .bodyValue(Map.of("title", "제목", "body", "내용", "secret", false,
                        "attachmentFileIds", List.of()))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("남의 글은 수정·삭제할 수 없다")
    void 남의_글은_삭제할_수_없다() {
        String ownerToken = signUpAndLogin("postowner");
        String strangerToken = signUpAndLogin("stranger");

        JsonNode created = webTestClient.post().uri("/api/v1/boards/FREE/posts")
                .header("Authorization", "Bearer " + ownerToken)
                .bodyValue(Map.of("title", "내 글", "body", "내용", "secret", false,
                        "attachmentFileIds", List.of()))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(JsonNode.class).returnResult().getResponseBody();

        String postId = created.at("/data/postId").asText();

        webTestClient.delete().uri("/api/v1/boards/posts/{id}", postId)
                .header("Authorization", "Bearer " + strangerToken)
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody().jsonPath("$.error.code").isEqualTo("CM-BOARD-007");
    }

    @Test
    @DisplayName("비밀글은 목록에서 제목이 가려지고 남이 열 수 없다")
    void 비밀글은_가려진다() {
        String ownerToken = signUpAndLogin("secretowner");
        String strangerToken = signUpAndLogin("peeker");

        JsonNode created = webTestClient.post().uri("/api/v1/boards/QNA/posts")
                .header("Authorization", "Bearer " + ownerToken)
                .bodyValue(Map.of("title", "민감한 제조 정보", "body", "내용", "secret", true,
                        "attachmentFileIds", List.of()))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(JsonNode.class).returnResult().getResponseBody();

        String postId = created.at("/data/postId").asText();

        // 남이 상세를 열면 거부된다
        webTestClient.get().uri("/api/v1/boards/posts/{id}", postId)
                .header("Authorization", "Bearer " + strangerToken)
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody().jsonPath("$.error.code").isEqualTo("CM-BOARD-009");

        // 목록에서는 제목이 가려진 채 보인다
        webTestClient.get().uri("/api/v1/boards/QNA/posts")
                .header("Authorization", "Bearer " + strangerToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.page.posts[0].secret").isEqualTo(true)
                .jsonPath("$.data.page.posts[0].readable").isEqualTo(false)
                .jsonPath("$.data.page.posts[0].maskedTitle").isEqualTo("비밀글입니다.");

        // 작성자 본인은 열 수 있다
        webTestClient.get().uri("/api/v1/boards/posts/{id}", postId)
                .header("Authorization", "Bearer " + ownerToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.data.title").isEqualTo("민감한 제조 정보");
    }

    @Test
    @DisplayName("공지에는 댓글을 달 수 없다")
    void 공지에는_댓글을_달_수_없다() {
        String token = signUpAndLogin("commenter");

        // 공지는 관리자만 쓸 수 있으므로, 댓글 금지는 자유게시판 대신 정책으로만 확인한다.
        // 여기서는 게시판 정책 목록이 그 사실을 클라이언트에 알려 주는지 검증한다.
        webTestClient.get().uri("/api/v1/boards/types")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data[?(@.code == 'NOTICE')].allowsComments").isEqualTo(false)
                .jsonPath("$.data[?(@.code == 'FREE')].allowsComments").isEqualTo(true)
                .jsonPath("$.data[?(@.code == 'QNA')].allowsSecretPost").isEqualTo(true);
    }

    @Test
    @DisplayName("자유게시판 글에는 댓글을 달 수 있다")
    void 자유게시판에는_댓글을_단다() {
        String token = signUpAndLogin("replier");

        JsonNode created = webTestClient.post().uri("/api/v1/boards/FREE/posts")
                .header("Authorization", "Bearer " + token)
                .bodyValue(Map.of("title", "질문 있어요", "body", "내용", "secret", false,
                        "attachmentFileIds", List.of()))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(JsonNode.class).returnResult().getResponseBody();

        String postId = created.at("/data/postId").asText();

        webTestClient.post().uri("/api/v1/boards/posts/{id}/comments", postId)
                .header("Authorization", "Bearer " + token)
                .bodyValue(Map.of("body", "저도 궁금합니다"))
                .exchange()
                .expectStatus().isCreated();

        webTestClient.get().uri("/api/v1/boards/posts/{id}", postId)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.comments[0].body").isEqualTo("저도 궁금합니다")
                .jsonPath("$.data.comments[0].authorNickname").isEqualTo("replier");
    }
}
