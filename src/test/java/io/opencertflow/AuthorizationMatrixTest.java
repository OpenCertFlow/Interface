package io.opencertflow;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * <b>인가 매트릭스.</b> 어떤 경로가 비로그인에 열려 있고 어떤 경로가 닫혀 있는지를 고정한다.
 *
 * <p>기존 통합 테스트는 각 기능의 <i>정상 경로</i>만 본다 — "로그인한 사용자가 초안을 저장할 수
 * 있다"는 확인하지만 "비로그인 사용자가 저장할 수 없다"는 아무도 확인하지 않았다. 접근 제어는
 * 경로 패턴 30줄에 걸려 있고 순서에 의존하는데, 그 30줄을 검증하는 테스트가 없었다.
 *
 * <p>이 테스트가 있어야 {@code anyExchange().denyAll()} 전환 같은 변경을 안전하게 할 수 있다.
 * 규칙을 고쳤을 때 무엇이 열리고 닫히는지 즉시 드러나기 때문이다.
 *
 * <p><b>검사하는 것은 인가뿐이다.</b> 공개 경로가 400·404·405를 돌려주는 것은 정상이다 — 본문 없이
 * 호출했으니 당연하다. 문제는 <b>401</b>이다. 그래서 "401인가 아닌가"만 본다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("local")
@Testcontainers
class AuthorizationMatrixTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    WebTestClient webTestClient;

    /**
     * 비로그인으로 열려 있어야 하는 경로.
     *
     * <p>여기 있는 것은 전부 <b>의도적으로</b> 공개된 것이다. 새 엔드포인트를 추가했는데 이 목록에
     * 없으면 닫혀 있어야 정상이다 — 열어야 한다면 여기에 한 줄 추가하는 것이 그 판단을 남기는 방법이다.
     */
    static Stream<Arguments> 공개_경로() {
        return Stream.of(
                // 인증 — 로그인하려면 로그인 API가 열려 있어야 한다
                Arguments.of(HttpMethod.POST, "/api/v1/auth/login"),
                Arguments.of(HttpMethod.POST, "/api/v1/auth/signup"),
                Arguments.of(HttpMethod.POST, "/api/v1/auth/refresh"),
                Arguments.of(HttpMethod.POST, "/api/v1/auth/email/code"),
                Arguments.of(HttpMethod.POST, "/api/v1/auth/password/reset-request"),

                // 진단 — 앱 설치 후 로그인 없이 바로 진단할 수 있어야 한다(기획 전제)
                Arguments.of(HttpMethod.POST, "/api/v1/diagnoses"),
                Arguments.of(HttpMethod.GET, "/api/v1/diagnoses/1"),
                Arguments.of(HttpMethod.POST, "/api/v1/diagnoses/1/simulations"),
                Arguments.of(HttpMethod.GET, "/api/v1/diagnoses/1/remediation-plan"),
                Arguments.of(HttpMethod.GET, "/api/v1/product-groups"),

                // 상담 접수 — 소공인이 로그인 없이 신청한다
                Arguments.of(HttpMethod.POST, "/api/v1/consulting-leads"),
                Arguments.of(HttpMethod.GET, "/api/v1/consulting-leads/1/messages"),

                // 열람 전용
                Arguments.of(HttpMethod.GET, "/api/v1/boards/types"),
                Arguments.of(HttpMethod.GET, "/api/v1/boards/NOTICE/posts"),
                Arguments.of(HttpMethod.GET, "/api/v1/documents/templates"),
                Arguments.of(HttpMethod.GET, "/api/v1/terms"),
                Arguments.of(HttpMethod.GET, "/api/v1/report-phrases"),

                // 운영 · 문서
                Arguments.of(HttpMethod.GET, "/actuator/health"),
                Arguments.of(HttpMethod.GET, "/v3/api-docs"));
    }

    /**
     * 비로그인이면 401이어야 하는 경로.
     *
     * <p>관리자·컨설턴트 전용 경로도 여기 포함된다. 권한 부족(403)은 인증을 통과한 뒤의 이야기이고,
     * 토큰이 아예 없으면 401이 맞다.
     */
    static Stream<Arguments> 보호된_경로() {
        return Stream.of(
                // 마이페이지
                Arguments.of(HttpMethod.GET, "/api/v1/me"),
                Arguments.of(HttpMethod.DELETE, "/api/v1/me"),
                Arguments.of(HttpMethod.PATCH, "/api/v1/me/nickname"),
                Arguments.of(HttpMethod.PATCH, "/api/v1/me/password"),
                Arguments.of(HttpMethod.GET, "/api/v1/me/notifications"),
                Arguments.of(HttpMethod.GET, "/api/v1/me/consulting-leads"),

                // 본인 소유 진단 — 이력·초안·재진단·삭제
                Arguments.of(HttpMethod.GET, "/api/v1/diagnoses/mine"),
                Arguments.of(HttpMethod.GET, "/api/v1/diagnoses/drafts"),
                Arguments.of(HttpMethod.POST, "/api/v1/diagnoses/drafts"),
                Arguments.of(HttpMethod.GET, "/api/v1/diagnoses/drafts/1"),
                Arguments.of(HttpMethod.DELETE, "/api/v1/diagnoses/1"),
                Arguments.of(HttpMethod.GET, "/api/v1/diagnoses/1/input"),
                Arguments.of(HttpMethod.GET, "/api/v1/diagnoses/1/compare"),
                Arguments.of(HttpMethod.POST, "/api/v1/diagnoses/1/rediagnose"),

                // 쓰기 — 게시판 · 파일 · 문서 발급
                Arguments.of(HttpMethod.POST, "/api/v1/boards/NOTICE/posts"),
                Arguments.of(HttpMethod.PUT, "/api/v1/boards/posts/1"),
                Arguments.of(HttpMethod.DELETE, "/api/v1/boards/posts/1"),
                Arguments.of(HttpMethod.POST, "/api/v1/boards/posts/1/comments"),
                Arguments.of(HttpMethod.POST, "/api/v1/files"),
                Arguments.of(HttpMethod.DELETE, "/api/v1/files/1"),
                Arguments.of(HttpMethod.POST, "/api/v1/documents/issues"),
                Arguments.of(HttpMethod.GET, "/api/v1/documents/issues"),

                // 관리자
                Arguments.of(HttpMethod.GET, "/api/v1/admin/users"),
                Arguments.of(HttpMethod.GET, "/api/v1/admin/dashboard"),
                Arguments.of(HttpMethod.GET, "/api/v1/admin/audit-logs"),
                Arguments.of(HttpMethod.GET, "/api/v1/admin/rule-sets"),
                Arguments.of(HttpMethod.POST, "/api/v1/admin/rule-sets"),
                Arguments.of(HttpMethod.GET, "/api/v1/admin/ai-fallback"),
                Arguments.of(HttpMethod.GET, "/api/v1/admin/document-weights"),
                Arguments.of(HttpMethod.GET, "/api/v1/admin/official-documents"),
                Arguments.of(HttpMethod.GET, "/api/v1/admin/report-phrases"),
                Arguments.of(HttpMethod.GET, "/api/v1/admin/evidence-feedback"),

                // 컨설턴트
                Arguments.of(HttpMethod.GET, "/api/v1/consulting/leads"),
                Arguments.of(HttpMethod.GET, "/api/v1/consulting/leads/1"),
                Arguments.of(HttpMethod.POST, "/api/v1/consulting/leads/1/assign"),
                Arguments.of(HttpMethod.GET, "/api/v1/rule-sets"),
                Arguments.of(HttpMethod.GET, "/api/v1/rule-sets/1"));
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("공개_경로")
    @DisplayName("공개 경로는 비로그인으로도 401을 받지 않는다")
    void 공개_경로는_인증을_요구하지_않는다(HttpMethod method, String path) {
        HttpStatus status = call(method, path);

        assertThat(status)
                .as("%s %s 는 공개 경로인데 인증을 요구한다", method, path)
                .isNotEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("보호된_경로")
    @DisplayName("보호된 경로는 비로그인이면 401을 돌려준다")
    void 보호된_경로는_인증을_요구한다(HttpMethod method, String path) {
        HttpStatus status = call(method, path);

        assertThat(status)
                .as("%s %s 가 비로그인에 열려 있다", method, path)
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    /** 본문 없이 호출한다. 여기서 궁금한 것은 시큐리티 필터의 판단뿐이다. */
    private HttpStatus call(HttpMethod method, String path) {
        return HttpStatus.valueOf(webTestClient
                .method(method)
                .uri(path)
                .exchange()
                .returnResult(Void.class)
                .getStatus()
                .value());
    }
}
