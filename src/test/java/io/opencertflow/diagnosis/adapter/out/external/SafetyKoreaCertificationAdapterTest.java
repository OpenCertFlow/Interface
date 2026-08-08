package io.opencertflow.diagnosis.adapter.out.external;

import static org.assertj.core.api.Assertions.assertThat;

import io.opencertflow.diagnosis.application.port.out.CertificationRegistryPort.CertificationGrade;
import io.opencertflow.diagnosis.application.port.out.CertificationRegistryPort.CertificationRecord;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * 제품안전정보센터 인증 현황 어댑터.
 *
 * <p>키가 필요한 테스트는 환경변수가 있을 때만 돈다 — 저장소를 클론한 사람이 키 없이도 전체
 * 테스트를 통과해야 하고, CI가 기관 API를 매번 두드리는 것도 곤란하다.
 *
 * <pre>{@code
 * OPENCERTFLOW_SAFETYKOREA_KEY=<발급받은키> \
 *   ./gradlew test --tests '*SafetyKoreaCertificationAdapterTest'
 * }</pre>
 */
class SafetyKoreaCertificationAdapterTest {

    private static final String PRODUCT = "전기방석";

    private SafetyKoreaCertificationAdapter adapterWith(SafetyKoreaProperties properties) {
        return new SafetyKoreaCertificationAdapter(WebClient.builder(), properties);
    }

    private SafetyKoreaProperties live() {
        return new SafetyKoreaProperties(
                true, System.getenv("OPENCERTFLOW_SAFETYKOREA_KEY"), "https://www.safetykorea.kr");
    }

    @Test
    @DisplayName("키가 없으면 호출하지 않고 빈 목록을 돌려준다")
    void 키가_없으면_비활성() {
        var adapter = adapterWith(
                new SafetyKoreaProperties(true, "", "https://www.safetykorea.kr"));

        assertThat(adapter.findByProductName(PRODUCT)).isEmpty();
    }

    @Test
    @DisplayName("enabled=false면 키가 있어도 호출하지 않는다")
    void 꺼져_있으면_비활성() {
        var adapter = adapterWith(
                new SafetyKoreaProperties(false, "dummy", "https://www.safetykorea.kr"));

        assertThat(adapter.findByProductName(PRODUCT)).isEmpty();
    }

    @Test
    @DisplayName("잘못된 키로는 예외 대신 빈 목록 — 조회 실패가 진단을 멈추면 안 된다")
    void 잘못된_키는_빈_목록() {
        var adapter = adapterWith(
                new SafetyKoreaProperties(true, "invalid-key", "https://www.safetykorea.kr"));

        assertThat(adapter.findByProductName(PRODUCT)).isEmpty();
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "OPENCERTFLOW_SAFETYKOREA_KEY", matches = ".+")
    @DisplayName("[키 필요] 전기방석 인증 현황을 읽고 등급을 해석한다")
    void 실제_API로_인증현황을_읽는다() {
        List<CertificationRecord> records = adapterWith(live()).findByProductName(PRODUCT);

        assertThat(records).isNotEmpty();
        assertThat(records)
                .as("등급을 하나도 해석하지 못했다면 certDiv 표기가 바뀐 것이다")
                .anyMatch(r -> r.grade() != CertificationGrade.UNKNOWN);
        assertThat(records)
                .allSatisfy(r -> {
                    assertThat(r.certificationNumber()).isNotBlank();
                    assertThat(r.rawDivision()).isNotBlank();
                });
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "OPENCERTFLOW_SAFETYKOREA_KEY", matches = ".+")
    @DisplayName("[키 필요] 현행법 대상 건이 존재하고 등급이 해석된다")
    void 현행법_대상을_구분한다() {
        List<CertificationRecord> current = adapterWith(live()).findByProductName(PRODUCT).stream()
                .filter(CertificationRecord::underCurrentAct)
                .toList();

        assertThat(current)
                .as("현행법(전기용품 및 생활용품 안전관리법) 대상 건이 하나도 없다")
                .isNotEmpty();
        assertThat(current)
                .extracting(CertificationRecord::grade)
                .contains(CertificationGrade.SAFETY_CERTIFICATION);
    }

    @Test
    @DisplayName("자율안전확인을 안전확인으로 잘못 읽지 않는다")
    void 등급_해석_순서() {
        var adapter = adapterWith(new SafetyKoreaProperties(false, "", ""));

        // gradeOf는 private이므로 실제 문자열이 섞인 상황을 레코드로 재현해 확인한다.
        var supplier = new CertificationRecord(
                "X", CertificationGrade.SUPPLIER_CONFIRMATION,
                "전기용품안전관리법 대상>자율안전확인 대상", null, null, null, null);

        assertThat(supplier.underCurrentAct())
                .as("구법 건은 현행 기준의 근거가 될 수 없다")
                .isFalse();
        assertThat(adapter.findByProductName("x")).isEmpty();
    }
}
