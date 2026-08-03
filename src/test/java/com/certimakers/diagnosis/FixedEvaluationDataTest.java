package com.certimakers.diagnosis;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * 고정 평가데이터 구동 테스트.
 *
 * <p>기획서 2.6·5.3이 약속한 "정상·모름·미확인·모순·근거부족·AI 장애·권한오류 사례를 고정
 * 평가데이터로 검증"을 실제로 수행한다. 케이스는 코드가 아니라
 * {@code src/test/resources/evaluation/cases.json}에 있다 — 기획·QA가 코드를 건드리지 않고
 * 케이스를 늘릴 수 있어야 하고, 심사에서 "무엇으로 검증했는가"에 파일 하나로 답할 수 있어야 한다.
 *
 * <p>기대값은 <b>명시한 것만</b> 검사한다. 무관한 값까지 고정하면 관계없는 변경에 테스트가
 * 깨지고, 그러면 팀은 테스트를 믿지 않게 된다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("local")
@Testcontainers
class FixedEvaluationDataTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

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

    static List<JsonNode> cases() throws IOException {
        try (InputStream stream = FixedEvaluationDataTest.class
                .getResourceAsStream("/evaluation/cases.json")) {
            assertThat(stream).as("평가데이터 파일이 있어야 한다").isNotNull();
            JsonNode root = MAPPER.readTree(stream);
            List<JsonNode> list = new ArrayList<>();
            root.forEach(list::add);
            assertThat(list).as("평가 케이스가 비어 있으면 안 된다").isNotEmpty();
            return list;
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("cases")
    @DisplayName("고정 평가데이터 케이스가 기대대로 동작한다")
    void evaluate(JsonNode testCase) {
        String id = testCase.path("id").asText();
        JsonNode expect = testCase.path("expect");

        if (testCase.hasNonNull("lookupOnly")) {
            webTestClient.get().uri("/api/v1/diagnoses/{id}", testCase.get("lookupOnly").asText())
                    .exchange()
                    .expectStatus().isEqualTo(expect.path("httpStatus").asInt(404));
            return;
        }

        WebTestClient.ResponseSpec response = webTestClient.post().uri("/api/v1/diagnoses")
                .bodyValue(MAPPER.convertValue(testCase.get("request"), Object.class))
                .exchange();

        if (expect.hasNonNull("httpStatus")) {
            response.expectStatus().isEqualTo(expect.get("httpStatus").asInt());
            return;
        }

        // 진단 생성은 201을 돌려준다. 케이스가 상태코드를 명시하지 않았다면 성공 여부만 본다.
        JsonNode body = response.expectStatus().is2xxSuccessful()
                .expectBody(JsonNode.class).returnResult().getResponseBody();
        assertThat(body).as("%s: 응답 본문", id).isNotNull();
        JsonNode data = body.path("data");

        assertExpectations(id, data, expect);
    }

    private void assertExpectations(String id, JsonNode data, JsonNode expect) {
        if (expect.hasNonNull("minCandidates")) {
            assertThat(data.path("candidates").size())
                    .as("%s: 인증 검토 후보 수 하한", id)
                    .isGreaterThanOrEqualTo(expect.get("minCandidates").asInt());
        }
        if (expect.hasNonNull("maxCandidates")) {
            assertThat(data.path("candidates").size())
                    .as("%s: 인증 검토 후보 수 상한", id)
                    .isLessThanOrEqualTo(expect.get("maxCandidates").asInt());
        }
        if (expect.hasNonNull("scoreApplicable")) {
            assertThat(data.path("score").path("applicable").asBoolean())
                    .as("%s: 준비도 산정 가능 여부", id)
                    .isEqualTo(expect.get("scoreApplicable").asBoolean());
        }
        if (expect.hasNonNull("minScore")) {
            assertThat(data.path("score").path("percentage").asInt())
                    .as("%s: 준비도 하한", id)
                    .isGreaterThanOrEqualTo(expect.get("minScore").asInt());
        }
        if (expect.hasNonNull("maxScore")) {
            assertThat(data.path("score").path("percentage").asInt())
                    .as("%s: 준비도 상한", id)
                    .isLessThanOrEqualTo(expect.get("maxScore").asInt());
        }
        if (expect.hasNonNull("unknownDocuments")) {
            assertThat(data.path("documentSummary").path("unknown").asInt())
                    .as("%s: '확인 중'으로 분류된 서류 수", id)
                    .isEqualTo(expect.get("unknownDocuments").asInt());
        }
        if (expect.hasNonNull("absentDocuments")) {
            assertThat(data.path("documentSummary").path("absent").asInt())
                    .as("%s: '누락'으로 분류된 서류 수", id)
                    .isEqualTo(expect.get("absentDocuments").asInt());
        }
        if (expect.hasNonNull("minExpertReviewItems")) {
            assertThat(data.path("expertReviewItems").size())
                    .as("%s: 전문가 확인 항목 수", id)
                    .isGreaterThanOrEqualTo(expect.get("minExpertReviewItems").asInt());
        }
        if (expect.hasNonNull("evidenceDegraded")) {
            assertThat(data.path("degraded").path("evidence").asBoolean())
                    .as("%s: 근거 저하 플래그", id)
                    .isEqualTo(expect.get("evidenceDegraded").asBoolean());
        }
        assertEvidenceInvariant(id, data);
    }

    /**
     * 케이스가 무엇을 기대하든 항상 지켜야 하는 규칙: <b>근거를 붙였어야 하는데 못 붙였으면
     * 그 사실이 응답에 드러나야 한다.</b>
     *
     * <p>후보가 없으면 검색할 것이 없으므로 저하가 아니다. 후보가 있는데 근거가 비었다면
     * 검색이 실패했거나 색인에 그 제품군 문서가 없는 것이고, 둘 다 사용자에게 알려야 한다.
     * 근거란이 조용히 비면 "근거가 필요 없는 제품"으로 오해된다.
     */
    private void assertEvidenceInvariant(String id, JsonNode data) {
        if (data.path("candidates").isEmpty() || !data.path("evidences").isEmpty()) {
            return;
        }
        assertThat(data.path("degraded").path("evidence").asBoolean())
                .as("%s: 후보가 있는데 근거가 0건이면 degraded.evidence가 참이어야 한다", id)
                .isTrue();
    }
}
