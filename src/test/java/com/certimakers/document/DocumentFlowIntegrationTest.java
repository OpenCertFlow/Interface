package com.certimakers.document;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
 * 문서 발급·PDF 엔드투엔드 검증.
 *
 * <p>발급은 <b>세 컨텍스트를 가로지른다</b> — 문서(값 검증)·공용 PDF(렌더링)·파일(저장). 각 단위
 * 테스트가 통과해도 이 연결이 어긋나면 기능은 동작하지 않으므로, 발급된 PDF를 실제로 내려받아
 * 확인한다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("local")
@Testcontainers
class DocumentFlowIntegrationTest {

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

    private static Map<String, String> selfDeclarationValues() {
        return Map.of(
                "companyName", "인증메이커스",
                "businessNumber", "123-45-67890",
                "representative", "홍길동",
                "productName", "가정용 헤어드라이어",
                "modelName", "CM-100",
                "ratedVoltage", "220",
                "powerConsumption", "1200",
                "declarationDate", "2026-08-10");
    }

    @Test
    @DisplayName("양식 목록은 입력 항목까지 알려 준다 — 클라이언트가 화면을 서버 정의대로 그린다")
    void 양식_목록에_입력_항목이_포함된다() {
        webTestClient.get().uri("/api/v1/documents/templates")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data[?(@.code == 'SELF_DECLARATION')].displayName")
                .isEqualTo("자기적합성 선언서(초안)")
                .jsonPath("$.data[0].fields").isNotEmpty();
    }

    @Test
    @DisplayName("양식을 채워 발급하면 한글 PDF가 생성되고 내려받을 수 있다")
    void 문서를_발급하고_PDF를_내려받는다() {
        String token = signUpAndLogin("issuer");

        JsonNode issued = webTestClient.post().uri("/api/v1/documents/issues")
                .header("Authorization", "Bearer " + token)
                .bodyValue(Map.of(
                        "templateCode", "SELF_DECLARATION",
                        "values", selfDeclarationValues()))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(JsonNode.class).returnResult().getResponseBody();

        String downloadUrl = issued.at("/data/downloadUrl").asText();
        assertThat(downloadUrl).startsWith("/api/v1/files/");

        // 발급 PDF는 비공개다 — 발급자 본인 인증 없이는 못 받는다
        byte[] pdf = webTestClient.get().uri(downloadUrl)
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody().returnResult().getResponseBody();

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 5, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF-");
        // 한글 폰트가 실제로 실렸는지 — 없으면 글자가 조용히 사라진다
        assertThat(new String(pdf, StandardCharsets.ISO_8859_1)).contains("HYSMyeongJo-Medium");
    }

    @Test
    @DisplayName("발급받은 PDF는 본인이 아니면 내려받을 수 없다")
    void 발급_PDF는_본인만_내려받는다() {
        String ownerToken = signUpAndLogin("pdfowner");
        String strangerToken = signUpAndLogin("pdfstranger");

        JsonNode issued = webTestClient.post().uri("/api/v1/documents/issues")
                .header("Authorization", "Bearer " + ownerToken)
                .bodyValue(Map.of("templateCode", "SELF_DECLARATION", "values", selfDeclarationValues()))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(JsonNode.class).returnResult().getResponseBody();

        String downloadUrl = issued.at("/data/downloadUrl").asText();

        // 비로그인은 거부됨
        webTestClient.get().uri(downloadUrl)
                .exchange()
                .expectStatus().isEqualTo(409);

        // 남도 거부됨
        webTestClient.get().uri(downloadUrl)
                .header("Authorization", "Bearer " + strangerToken)
                .exchange()
                .expectStatus().isEqualTo(409);
    }

    @Test
    @DisplayName("필수 항목이 비면 어떤 항목인지 알려 주며 거부한다")
    void 필수_항목이_비면_거부한다() {
        String token = signUpAndLogin("incomplete");
        Map<String, String> missing = Map.of("companyName", "인증메이커스");

        webTestClient.post().uri("/api/v1/documents/issues")
                .header("Authorization", "Bearer " + token)
                .bodyValue(Map.of("templateCode", "SELF_DECLARATION", "values", missing))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.error.code").isEqualTo("CM-DOC-002");
    }

    @Test
    @DisplayName("발급 이력은 본인 것만 보인다")
    void 발급_이력은_본인_것만_보인다() {
        String ownerToken = signUpAndLogin("docowner");
        String strangerToken = signUpAndLogin("docstranger");

        JsonNode issued = webTestClient.post().uri("/api/v1/documents/issues")
                .header("Authorization", "Bearer " + ownerToken)
                .bodyValue(Map.of("templateCode", "SELF_DECLARATION", "values", selfDeclarationValues()))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(JsonNode.class).returnResult().getResponseBody();

        String documentId = issued.at("/data/documentId").asText();

        // 남이 상세를 열면 거부된다 — 사업자등록번호가 담겨 있다
        webTestClient.get().uri("/api/v1/documents/issues/{id}", documentId)
                .header("Authorization", "Bearer " + strangerToken)
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody().jsonPath("$.error.code").isEqualTo("CM-DOC-006");

        // 남의 목록에는 나타나지 않는다
        webTestClient.get().uri("/api/v1/documents/issues")
                .header("Authorization", "Bearer " + strangerToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.data").isEmpty();

        // 본인은 재발급을 위해 입력값까지 되받는다
        webTestClient.get().uri("/api/v1/documents/issues/{id}", documentId)
                .header("Authorization", "Bearer " + ownerToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.data.values.companyName").isEqualTo("인증메이커스");
    }

    @Test
    @DisplayName("로그인하지 않으면 발급할 수 없다")
    void 비로그인은_발급할_수_없다() {
        webTestClient.post().uri("/api/v1/documents/issues")
                .bodyValue(Map.of("templateCode", "SELF_DECLARATION", "values", selfDeclarationValues()))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("진단 리포트를 PDF로 내려받을 수 있다")
    void 진단_리포트를_PDF로_내려받는다() {
        JsonNode created = webTestClient.post().uri("/api/v1/diagnoses")
                .bodyValue(Map.of(
                        "productName", "가정용 헤어드라이어",
                        "productGroup", "SMALL_APPLIANCE",
                        "usesElectricity", true,
                        "ratedVoltage", 220,
                        "powerConsumption", 1200,
                        "hasBattery", false,
                        "targetUser", "GENERAL",
                        "salesChannel", "ONLINE",
                        "materials", List.of("PLASTIC"),
                        "heldDocuments", List.of()))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(JsonNode.class).returnResult().getResponseBody();

        String diagnosisId = created.at("/data/id").asText();

        byte[] pdf = webTestClient.get().uri("/api/v1/diagnoses/{id}/report.pdf", diagnosisId)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType("application/pdf")
                .expectBody().returnResult().getResponseBody();

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 5, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF-");
        assertThat(new String(pdf, StandardCharsets.ISO_8859_1)).contains("HYSMyeongJo-Medium");
    }

    @Test
    @DisplayName("존재하지 않는 진단의 PDF는 404")
    void 없는_진단의_PDF는_404() {
        webTestClient.get()
                .uri("/api/v1/diagnoses/{id}/report.pdf", "999999999")
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    @DisplayName("관리자가 아니면 권한 부여 API를 쓸 수 없다")
    void 일반회원은_권한_부여를_할_수_없다() {
        String token = signUpAndLogin("notadmin");

        webTestClient.patch()
                .uri("/api/v1/admin/users/{id}/role", "999999999")
                .header("Authorization", "Bearer " + token)
                .bodyValue(Map.of("role", "ADMIN"))
                .exchange()
                .expectStatus().isForbidden();
    }
}
