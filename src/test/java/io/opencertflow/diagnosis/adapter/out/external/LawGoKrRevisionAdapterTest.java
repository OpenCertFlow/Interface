package io.opencertflow.diagnosis.adapter.out.external;

import static org.assertj.core.api.Assertions.assertThat;

import io.opencertflow.diagnosis.application.port.out.LawRevisionPort.LawRevision;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * 법제처 국가법령정보 API 어댑터.
 *
 * <p>키가 필요한 테스트는 <b>환경변수가 있을 때만</b> 돈다. 오픈소스 저장소를 클론한 사람이
 * 인증키 없이도 전체 테스트를 통과할 수 있어야 하고, CI가 남의 기관 API를 매번 두드리는 것도
 * 바람직하지 않다.
 *
 * <pre>{@code
 * OPENCERTFLOW_LAW_OC=<발급받은값> ./gradlew test --tests '*LawGoKrRevisionAdapterTest'
 * }</pre>
 *
 * <p>키 없이도 도는 테스트는 "키가 없으면 조용히 비활성인가"를 확인한다 — 그것이 기본 동작이다.
 */
class LawGoKrRevisionAdapterTest {

    private static final String LAW = "전기용품 및 생활용품 안전관리법";

    private LawGoKrRevisionAdapter adapterWith(LawGoKrProperties properties) {
        return new LawGoKrRevisionAdapter(WebClient.builder(), properties);
    }

    @Test
    @DisplayName("키가 없으면 호출하지 않고 빈 값을 돌려준다")
    void 키가_없으면_비활성() {
        LawGoKrRevisionAdapter adapter =
                adapterWith(new LawGoKrProperties(true, "", "https://www.law.go.kr"));

        assertThat(adapter.findCurrent(LAW)).isEmpty();
    }

    @Test
    @DisplayName("enabled=false면 키가 있어도 호출하지 않는다")
    void 꺼져_있으면_비활성() {
        LawGoKrRevisionAdapter adapter =
                adapterWith(new LawGoKrProperties(false, "dummy", "https://www.law.go.kr"));

        assertThat(adapter.findCurrent(LAW)).isEmpty();
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "OPENCERTFLOW_LAW_OC", matches = ".+")
    @DisplayName("[키 필요] 실제 API에서 전기용품법의 공포·시행 정보를 읽는다")
    void 실제_API로_개정정보를_읽는다() {
        LawGoKrRevisionAdapter adapter = adapterWith(new LawGoKrProperties(
                true, System.getenv("OPENCERTFLOW_LAW_OC"), "https://www.law.go.kr"));

        Optional<LawRevision> found = adapter.findCurrent(LAW);

        assertThat(found).isPresent();
        LawRevision revision = found.get();
        assertThat(revision.lawName()).contains("전기용품");
        assertThat(revision.revisionNumber()).isNotBlank();
        assertThat(revision.promulgatedOn()).isNotNull();
        assertThat(revision.effectiveOn()).isNotNull();
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "OPENCERTFLOW_LAW_OC", matches = ".+")
    @DisplayName("[키 필요] 상세 링크에 인증키가 섞여 나가지 않는다")
    void 상세링크에_인증키가_없다() {
        String oc = System.getenv("OPENCERTFLOW_LAW_OC");
        LawGoKrRevisionAdapter adapter =
                adapterWith(new LawGoKrProperties(true, oc, "https://www.law.go.kr"));

        String detailUrl = adapter.findCurrent(LAW).orElseThrow().detailUrl();

        // API 응답의 '법령상세링크'는 /DRF/lawService.do?OC=<키>... 형태로 키를 품고 있다.
        // 그대로 저장·노출하면 키가 새므로, 어댑터가 공개 페이지 주소로 바꿔 준다.
        assertThat(detailUrl)
                .as("상세 링크에 인증키가 포함되어 있다 — 저장·노출 시 유출된다")
                .doesNotContain(oc)
                .doesNotContain("OC=")
                .contains("lsInfoP.do");
    }
}
