package com.certimakers.diagnosis;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
 * 두 제품군(드라이기·전기방석)이 실제로 다르게 분기하는지 검증한다.
 *
 * <p>제품군을 추가한 목적이 "제품별 조건 분기를 보여 주는 것"이므로, 같은 흐름에서 <b>다른 결과</b>가
 * 나오는지가 핵심이다. 시드 룰셋이 잘못 로딩되거나 발열 속성이 전달되지 않으면 두 제품이 같은
 * 결과를 내고, 그러면 제품군을 나눈 의미가 없다.
 *
 * <p>전기방석의 인증 등급·서류는 공식 확인 전이라 단정하지 않는다. 이 테스트도 "정확히 어떤
 * 인증이 나오는가"가 아니라 "판단할 수 없음을 정직하게 알리는가"를 확인한다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("local")
@Testcontainers
class TwoProductGroupIntegrationTest {

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

    private static Map<String, Object> hairDryer() {
        Map<String, Object> request = new HashMap<>();
        request.put("productName", "가정용 헤어드라이어");
        request.put("productGroup", "SMALL_APPLIANCE");
        request.put("usesElectricity", true);
        request.put("ratedVoltage", 220);
        request.put("powerConsumption", 1200);
        request.put("hasBattery", false);
        request.put("targetUser", "GENERAL");
        request.put("salesChannel", "ONLINE");
        request.put("materials", List.of("PLASTIC", "METAL"));
        request.put("heldDocuments", List.of());
        return request;
    }

    /**
     * 봉제 소공인이 방석에 열선·온도조절기·어댑터를 결합한 제품(F-APP-014~018 재정의 모델).
     * {@code hasController}가 참이면 온도조절기 있음(3단), 거짓이면 없음. 표면온도가 있으면 측정값,
     * 없으면 출처 '모름'으로 둔다.
     */
    private static Map<String, Object> heatingPad(
            Boolean hasController, Integer surfaceTemperature) {
        Map<String, Object> request = new HashMap<>();
        request.put("productName", "보온용 전기방석");
        request.put("productGroup", "ELECTRIC_HEATING_PAD");
        request.put("usesElectricity", true);
        request.put("ratedVoltage", 220);
        request.put("powerConsumption", 60);
        request.put("hasBattery", false);
        request.put("targetUser", "GENERAL");
        request.put("salesChannel", "ONLINE");
        request.put("materials", List.of("TEXTILE"));
        request.put("heldDocuments", List.of());
        request.put("bodyContactType", "DIRECT_SKIN");
        request.put("controllerStatus", Boolean.TRUE.equals(hasController) ? "PRESENT" : "ABSENT");
        if (Boolean.TRUE.equals(hasController)) {
            request.put("adjustmentMode", "STEP");
            request.put("adjustmentSteps", 3);
        }
        request.put("maxSurfaceTemperatureCelsius", surfaceTemperature);
        request.put("temperatureSource", surfaceTemperature == null ? "UNKNOWN" : "MEASURED");
        // 발열 상세(F-APP-014~018, 결정문 §5.2) — 안전 요건을 갖춘 표준 구성. 별도 어댑터는 없다.
        request.put("medicalUseClaim", false);
        request.put("autoShutOff", true);
        request.put("autoShutOffMinutes", 30);
        request.put("overheatProtection", true);
        request.put("temperatureLimitDevice", true);
        request.put("removableCover", true);
        request.put("washable", true);
        request.put("separableElectricParts", true);
        request.put("hasSeparateAdapter", false);
        return request;
    }

    private JsonNode diagnose(Map<String, Object> request) {
        return webTestClient.post().uri("/api/v1/diagnoses")
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(JsonNode.class)
                .returnResult().getResponseBody();
    }

    @Test
    @DisplayName("제품군 메타데이터가 두 제품군과 각각의 입력 항목을 알려 준다")
    void 제품군_메타데이터를_내려준다() {
        webTestClient.get().uri("/api/v1/product-groups")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data[?(@.code == 'SMALL_APPLIANCE')].displayName").isEqualTo("소형가전")
                .jsonPath("$.data[?(@.code == 'ELECTRIC_HEATING_PAD')].displayName")
                .isEqualTo("일반 보온용 전기방석")
                // 전기방석에만 발열 항목이 있어야 한다
                .jsonPath("$.data[?(@.code == 'ELECTRIC_HEATING_PAD')].fields[?(@.code == 'maxSurfaceTemperatureCelsius')].required")
                .isEqualTo(false)
                // 전압은 전기 사용에 의존한다
                .jsonPath("$.data[?(@.code == 'SMALL_APPLIANCE')].fields[?(@.code == 'ratedVoltage')].dependsOn")
                .isEqualTo("usesElectricity");
    }

    @Test
    @DisplayName("드라이기는 기존대로 안전확인 후보가 나온다 — 제품군 추가가 기존 진단을 깨지 않았다")
    void 드라이기_진단은_그대로다() {
        JsonNode report = diagnose(hairDryer());

        assertThat(report.at("/data/candidates").toString())
                .contains("KC_SAFETY_CONFIRM_ELECTRIC", "SAFETY_CONFIRM");
        assertThat(report.at("/data/score/applicable").asBoolean()).isTrue();
    }

    @Test
    @DisplayName("전기방석은 드라이기와 다른 룰셋으로 평가된다")
    void 전기방석은_다른_룰셋을_쓴다() {
        JsonNode dryer = diagnose(hairDryer());
        JsonNode pad = diagnose(heatingPad(true, 45));

        // 서로 다른 제품군 룰셋이 매칭되었으므로 후보 구성이 다르다.
        assertThat(pad.at("/data/candidates").toString())
                .as("전기방석은 소형가전 안전확인 룰(R-SA-001)에 매칭되면 안 된다")
                .doesNotContain("KC_SAFETY_CONFIRM_ELECTRIC");
        assertThat(dryer.at("/data/candidates").toString())
                .contains("KC_SAFETY_CONFIRM_ELECTRIC");
    }

    @Test
    @DisplayName("전기방석은 공식 확인 전이라 '전문가 확인 필요'로 정직하게 안내한다")
    void 전기방석은_확인_전이라_전문가_확인으로_보낸다() {
        JsonNode report = diagnose(heatingPad(true, 45));

        String expertItems = report.at("/data/expertReviewItems").toString();
        assertThat(expertItems)
                .as("등급이 확인되지 않았으므로 근거 부재 사유로 안내해야 한다")
                .contains("NO_EVIDENCE");
        assertThat(expertItems).contains("인증 제도와 등급");
    }

    @Test
    @DisplayName("온도조절기가 없으면 과열 확인 항목이 추가된다 — 조건 분기가 실제로 동작한다")
    void 온도조절기_유무로_분기한다() {
        String withController =
                diagnose(heatingPad(true, 45)).at("/data/expertReviewItems").toString();
        String withoutController =
                diagnose(heatingPad(false, 45)).at("/data/expertReviewItems").toString();

        assertThat(withoutController).contains("온도조절기");
        assertThat(withController).doesNotContain("온도조절기");
    }

    @Test
    @DisplayName("표면온도를 모르면 진단을 막지 않고 확인 항목으로 안내한다")
    void 표면온도를_몰라도_진단은_된다() {
        JsonNode report = diagnose(heatingPad(true, null));

        assertThat(report.at("/data/status").asText()).startsWith("COMPLETED");
        assertThat(report.at("/data/expertReviewItems").toString())
                .contains("표면온도", "AMBIGUOUS_CONDITION");
    }

    @Test
    @DisplayName("발열 사양 없이 전기방석을 보내면 무엇이 빠졌는지 알려 준다")
    void 발열_사양이_빠지면_알려준다() {
        Map<String, Object> incomplete = heatingPad(true, 45);
        incomplete.remove("controllerStatus");

        webTestClient.post().uri("/api/v1/diagnoses")
                .bodyValue(incomplete)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.error.message").value(message ->
                        assertThat(message.toString()).contains("controllerStatus"));
    }

    @Test
    @DisplayName("저장 후 다시 읽어도 발열 사양이 보존된다")
    void 발열_사양이_왕복해도_보존된다() {
        String id = diagnose(heatingPad(false, 70)).at("/data/id").asText();

        JsonNode reloaded = webTestClient.get().uri("/api/v1/diagnoses/{id}", id)
                .exchange()
                .expectStatus().isOk()
                .expectBody(JsonNode.class).returnResult().getResponseBody();

        // 온도조절기 없음 분기가 재조회 후에도 유지되면 발열 사양이 온전히 저장·복원된 것이다.
        assertThat(reloaded.at("/data/expertReviewItems").toString()).contains("온도조절기");
    }

    @Test
    @DisplayName("신체 접촉 방식을 모르면 접촉 방식 확인 항목으로 안내한다 — 결정문 §5.2 '모름'")
    void 접촉_방식_모름은_확인으로_보낸다() {
        Map<String, Object> request = heatingPad(true, 45);
        request.put("bodyContactType", "UNKNOWN");

        String items = diagnose(request).at("/data/expertReviewItems").toString();
        assertThat(items).contains("닿는 방식", "AMBIGUOUS_CONDITION");
    }

    @Test
    @DisplayName("온도제한장치가 없으면 화상 위험 확인이 추가된다 — 과열 차단과 별개(결정문 §5.2)")
    void 온도제한장치_없음은_화상_위험_확인이다() {
        String withLimit =
                diagnose(heatingPad(true, 45)).at("/data/expertReviewItems").toString();
        Map<String, Object> without = heatingPad(true, 45);
        without.put("temperatureLimitDevice", false);
        String withoutLimit = diagnose(without).at("/data/expertReviewItems").toString();

        assertThat(withoutLimit).contains("상한을 제한하는 장치");
        assertThat(withLimit).doesNotContain("상한을 제한하는 장치");
    }

    @Test
    @DisplayName("온도 조절 방식이 '기타'면 조절 방식 확인 항목으로 안내한다 — 결정문 §5.2")
    void 조절_방식_기타는_확인으로_보낸다() {
        Map<String, Object> request = heatingPad(true, 45);
        request.put("adjustmentMode", "OTHER");
        request.remove("adjustmentSteps"); // 기타·연속이면 단계 수는 비어 있어야 한다(불변식)

        String items = diagnose(request).at("/data/expertReviewItems").toString();
        assertThat(items).contains("조절 방식이 확인되지", "AMBIGUOUS_CONDITION");
    }

    @Test
    @DisplayName("변경모델이면 기존 인증 범위 확인 항목이 뜬다(F-APP-008)")
    void 변경모델은_기존_인증_범위_확인이다() {
        Map<String, Object> request = hairDryer();
        request.put("isModifiedModel", true);

        String items = diagnose(request).at("/data/expertReviewItems").toString();
        assertThat(items).contains("기존 인증 범위");
    }

    @Test
    @DisplayName("수입·OEM·ODM 제품이면 제조 책임 확인 항목이 뜬다(F-APP-006)")
    void 수입_oem_odm은_제조_책임_확인이다() {
        Map<String, Object> oem = hairDryer();
        oem.put("manufacturingType", "OEM");
        assertThat(diagnose(oem).at("/data/expertReviewItems").toString())
                .contains("제조 책임");

        // 자체 제조면 그 항목이 없다 — 조건 분기가 실제로 동작한다.
        Map<String, Object> self = hairDryer();
        self.put("manufacturingType", "SELF_MADE");
        assertThat(diagnose(self).at("/data/expertReviewItems").toString())
                .doesNotContain("제조 책임");
    }
}
