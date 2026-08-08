package io.opencertflow;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * {@code openapi/openapi.json}이 실제 API와 일치하는지 검증하고, 필요하면 갱신한다.
 *
 * <p><b>왜 테스트인가.</b> 이 스펙 파일은 SDK의 원본이다 — TypeScript·Python·Kotlin 클라이언트가
 * 여기서 생성된다. 파일이 코드보다 낡으면 SDK 사용자는 존재하지 않는 필드를 부르게 되고, 그
 * 사실을 런타임에야 알게 된다. 사람이 기억해서 갱신하는 절차는 반드시 잊힌다.
 *
 * <p>스펙을 뽑으려면 애플리케이션 컨텍스트가 떠야 하고(springdoc이 런타임에 스캔한다) DB가
 * 필요하다. 그 조건을 이미 갖춘 곳이 통합 테스트다. 그래서 별도 배치가 아니라 테스트로 둔다 —
 * CI가 매번 돌리므로 드리프트가 쌓일 수 없다.
 *
 * <p>갱신: {@code ./gradlew updateOpenApiSpec}
 */
/*
 * webEnvironment는 MOCK이다. RANDOM_PORT로 네티를 띄우면 springdoc이 스펙을 만들려고 클래스
 * 파일을 읽는데(RandomAccessFile), 그것이 이벤트 루프에서 일어나 BlockHound가 정당하게 막는다.
 * 스펙 생성은 요청 처리 경로가 아니라 빌드 시점 작업이므로, 네티를 거치지 않고 컨텍스트에 직접
 * 바인딩해 테스트 스레드에서 돌린다. BlockHound의 방어선을 끄지 않으면서 목적을 달성하는 방법이다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureWebTestClient
@ActiveProfiles("local")
@Testcontainers
class OpenApiSpecTest {

    /** 저장소 루트 기준 경로. SDK 생성 태스크가 같은 파일을 읽는다. */
    private static final Path SPEC = Path.of("openapi", "openapi.json");

    /** 켜면 비교 대신 갱신한다. {@code updateOpenApiSpec} 태스크가 설정한다. */
    private static final boolean UPDATE = Boolean.getBoolean("updateOpenApiSpec");

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

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    @DisplayName("openapi/openapi.json이 실제 API와 일치한다 (updateOpenApiSpec=true면 갱신)")
    void openapi_스펙이_최신이다() throws IOException {
        JsonNode live = fetchSpec();

        // 정렬된 형태로 직렬화한다. 키 순서가 흔들리면 매번 diff가 나서 드리프트 검출이 무의미해진다.
        String rendered = mapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(mapper.readTree(
                        mapper.writer().withFeatures(
                                com.fasterxml.jackson.databind.SerializationFeature
                                        .ORDER_MAP_ENTRIES_BY_KEYS)
                                .writeValueAsString(live)))
                + System.lineSeparator();

        if (UPDATE) {
            Files.createDirectories(SPEC.getParent());
            Files.writeString(SPEC, rendered, StandardCharsets.UTF_8);
            return;
        }

        assertThat(SPEC)
                .as("openapi/openapi.json이 없습니다. ./gradlew updateOpenApiSpec 으로 생성하세요.")
                .exists();

        String committed = Files.readString(SPEC, StandardCharsets.UTF_8);
        assertThat(normalize(committed))
                .as("openapi/openapi.json이 실제 API와 다릅니다. "
                        + "./gradlew updateOpenApiSpec 을 돌리고 결과를 커밋하세요.")
                .isEqualTo(normalize(rendered));
    }

    /** 줄바꿈 차이(CRLF/LF)로 실패하지 않게 한다. 그건 드리프트가 아니다. */
    private String normalize(String json) {
        return json.replace("\r\n", "\n").trim();
    }

    private JsonNode fetchSpec() {
        return webTestClient.get()
                .uri("/v3/api-docs")
                .exchange()
                .expectStatus().isOk()
                .expectBody(JsonNode.class)
                .returnResult()
                .getResponseBody();
    }
}
