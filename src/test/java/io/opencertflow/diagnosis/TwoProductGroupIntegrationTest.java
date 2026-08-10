package io.opencertflow.diagnosis;

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
 * <p>전기방석의 인증 등급은 시행규칙 별표로 확인되어 있다 — 교류면 별표 3(안전인증),
 * 직류면 별표 5(공급자적합성확인). 둘의 무게가 전혀 다르므로(공장심사 유무) 등급 분기를
 * 여기서 고정한다. 전원 방식을 모르면 어느 쪽으로도 단정하지 않는다.
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
        request.put("applianceItem", "HAIR_CARE_DEVICE");
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
    @DisplayName("드라이기는 안전인증 후보가 나온다 — 모발관리기는 시행규칙 별표 3이다")
    void 드라이기_진단은_안전인증이다() {
        JsonNode report = diagnose(hairDryer());

        assertThat(report.at("/data/candidates").toString())
                .contains("KC_SAFETY_CERT_ELECTRIC", "SAFETY_CERT");
        assertThat(report.at("/data/score/applicable").asBoolean()).isTrue();
    }

    @Test
    @DisplayName("전기방석은 드라이기와 다른 룰셋으로 평가된다")
    void 전기방석은_다른_룰셋을_쓴다() {
        JsonNode dryer = diagnose(hairDryer());
        JsonNode pad = diagnose(heatingPad(true, 45));

        // 드라이기는 품목이 확인되어 안전인증이 나오고, 전기방석은 전원 방식을 보내지
        // 않았으므로 등급을 단정하지 않는다. 두 룰셋이 실제로 다르게 동작한다.
        assertThat(pad.at("/data/candidates").toString())
                .as("전기방석이 소형가전 품목 룰에 매칭되면 안 된다")
                .doesNotContain("KC_SAFETY_CERT_ELECTRIC");
        assertThat(dryer.at("/data/candidates").toString())
                .contains("KC_SAFETY_CERT_ELECTRIC");
    }

    @Test
    @DisplayName("교류 전기방석은 안전인증과 함께 공장심사를 미리 알린다")
    void 안전인증은_공장심사를_함께_알린다() {
        Map<String, Object> ac = heatingPad(true, 45);
        ac.put("powerSource", "AC");

        String expertItems = diagnose(ac).at("/data/expertReviewItems").toString();

        // 안전인증은 제품시험으로 끝나지 않는다. 공장심사를 뒤늦게 알면 일정과 비용이 함께 틀어진다.
        assertThat(expertItems).contains("공장 심사");
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

    // ── 전원 방식이 인증 등급을 가른다 ──────────────────────────────
    //
    // 「전기용품 및 생활용품 안전관리법 시행규칙」은 전기찜질기를 전원 방식에 따라 나눈다.
    //   별표 3 안전인증대상          10) 교류전원을 사용하는 전기찜질기, 발 보온기
    //   별표 5 공급자적합성확인대상  16) 직류전원을 사용하는 전기찜질기 및 발 보온기
    //
    // 둘의 무게가 전혀 다르다(공장심사 유무). 잘못 안내하면 소공인이 필요 없는 비용을 쓰거나,
    // 받아야 할 인증을 놓친다. 그래서 등급 분기는 반드시 테스트로 고정한다.

    @Test
    @DisplayName("교류전원 전기방석은 안전인증 대상이다 (시행규칙 별표 3 제10호)")
    void 교류_전기방석은_안전인증이다() {
        Map<String, Object> ac = heatingPad(true, 45);
        ac.put("powerSource", "AC");

        String candidates = diagnose(ac).at("/data/candidates").toString();

        assertThat(candidates).contains("SAFETY_CERT");
        assertThat(candidates)
                .as("교류 제품에 공급자적합성확인을 안내하면 받아야 할 인증을 놓치게 된다")
                .doesNotContain("SUPPLIER_DOC");
    }

    @Test
    @DisplayName("직류전원 전기방석은 공급자적합성확인 대상이다 (시행규칙 별표 5 제16호)")
    void 직류_전기방석은_공급자적합성확인이다() {
        Map<String, Object> dc = heatingPad(true, 45);
        dc.put("powerSource", "DC");

        String candidates = diagnose(dc).at("/data/candidates").toString();

        assertThat(candidates).contains("SUPPLIER_DOC");
        assertThat(candidates)
                .as("직류 제품에 안전인증을 안내하면 필요 없는 공장심사 비용을 물리게 된다")
                .doesNotContain("SAFETY_CERT");
    }

    // ── 소형가전은 품목이 인증 등급을 가른다 ────────────────────────
    //
    // 예전 룰은 "전기 사용 + 50V 초과"만 보고 소형가전 전체에 안전확인을 붙였다. 그래서
    // 헤어드라이어(별표 3 안전인증 대상)에 받아야 할 인증을 축소해 안내했다(#26).

    @Test
    @DisplayName("모발관리기(헤어드라이어)는 안전인증 대상이다 — 시행규칙 별표 3 제5호")
    void 모발관리기는_안전인증이다() {
        Map<String, Object> dryer = hairDryer();
        dryer.put("applianceItem", "HAIR_CARE_DEVICE");

        String candidates = diagnose(dryer).at("/data/candidates").toString();

        assertThat(candidates).contains("SAFETY_CERT");
        assertThat(candidates)
                .as("안전확인으로 안내하면 받아야 할 인증을 축소해 알려 주게 된다")
                .doesNotContain("SAFETY_CONFIRM");
    }

    @Test
    @DisplayName("전기청소기는 안전확인 대상이다 — 시행규칙 별표 4 제42호")
    void 전기청소기는_안전확인이다() {
        Map<String, Object> cleaner = hairDryer();
        cleaner.put("productName", "가정용 전기청소기");
        cleaner.put("applianceItem", "VACUUM_CLEANER");

        String candidates = diagnose(cleaner).at("/data/candidates").toString();

        assertThat(candidates).contains("SAFETY_CONFIRM");
        assertThat(candidates)
                .as("안전인증으로 안내하면 필요 없는 공장심사 비용을 물리게 된다")
                .doesNotContain("SAFETY_CERT");
    }

    @Test
    @DisplayName("품목을 모르면 등급을 단정하지 않는다")
    void 품목_모르면_등급을_고르지_않는다() {
        // applianceItem을 빼고 보낸다 — 이 필드를 모르는 기존 클라이언트가 그러하듯.
        Map<String, Object> unspecified = hairDryer();
        unspecified.remove("applianceItem");
        JsonNode report = diagnose(unspecified);

        assertThat(report.at("/data/expertReviewItems").toString()).contains("품목");
        assertThat(report.at("/data/candidates").toString())
                .as("모르는데 등급을 고르면 둘 중 하나는 반드시 틀린 안내가 된다")
                .doesNotContain("SAFETY_CERT")
                .doesNotContain("SAFETY_CONFIRM");
    }

    @Test
    @DisplayName("전원 방식을 모르면 등급을 단정하지 않고 확인을 요청한다")
    void 전원방식_모르면_판단하지_않는다() {
        // powerSource를 아예 보내지 않는다 — 기존 클라이언트가 그러하듯.
        JsonNode report = diagnose(heatingPad(true, 45));

        assertThat(report.at("/data/expertReviewItems").toString())
                .contains("전원 방식");
        assertThat(report.at("/data/candidates").toString())
                .as("모르는데 등급을 고르면 둘 중 하나는 반드시 틀린 안내가 된다")
                .doesNotContain("SAFETY_CERT")
                .doesNotContain("SUPPLIER_DOC");
    }
}
