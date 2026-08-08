package io.opencertflow.diagnosis.adapter.in.web;

import static io.opencertflow.diagnosis.domain.RuleSetFixtures.BIZ_LICENSE;
import static io.opencertflow.diagnosis.domain.RuleSetFixtures.TEST_REPORT;
import static org.assertj.core.api.Assertions.assertThat;

import io.opencertflow.diagnosis.domain.ProductProfileFixtures;
import io.opencertflow.diagnosis.domain.model.AdjustmentMode;
import io.opencertflow.diagnosis.domain.model.BodyContactType;
import io.opencertflow.diagnosis.domain.model.ControllerStatus;
import io.opencertflow.diagnosis.domain.model.ElectricalSpec;
import io.opencertflow.diagnosis.domain.model.HeatingSpec;
import io.opencertflow.diagnosis.domain.model.ManufacturingType;
import io.opencertflow.diagnosis.domain.model.MaterialType;
import io.opencertflow.diagnosis.domain.model.ProductGroup;
import io.opencertflow.diagnosis.domain.model.ProductProfile;
import io.opencertflow.diagnosis.domain.model.SalesChannel;
import io.opencertflow.diagnosis.domain.model.TargetUser;
import io.opencertflow.diagnosis.domain.model.TemperatureSource;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 진단 입력의 웹 DTO ↔ 도메인 변환 검증(F-APP-034 재진단 프리필).
 *
 * <p>핵심은 <b>왕복 테스트</b>다. {@link DiagnoseRequest}는 필드가 30개인데 대부분
 * {@code Boolean}·{@code Integer}·{@code String}이라, {@code toRequest}에서 인자 순서를
 * 바꿔 넣어도 컴파일이 통과한다. 자리가 어긋나면 재진단 화면에 엉뚱한 값이 채워지고
 * 사용자가 그대로 제출해 잘못된 진단이 나오는데, 예외는 하나도 안 난다.
 *
 * <p>원본 → {@code toRequest} → {@code toProfile} → 복원이 원본과 같은지 보면 필드 30개를
 * 한 줄로 검증할 수 있다({@code ProductProfile}이 record라 equals가 전 필드를 비교한다).
 */
class DiagnosisWebMapperTest {

    private final DiagnosisWebMapper mapper = new DiagnosisWebMapper();

    @Test
    @DisplayName("발열 사양이 없는 제품(헤어드라이어)은 왕복해도 원본과 같다")
    void 소형가전_왕복() {
        ProductProfile original = ProductProfileFixtures.hairDryer(Set.of(BIZ_LICENSE, TEST_REPORT));

        ProductProfile restored = mapper.toProfile(mapper.toRequest(original));

        assertThat(restored).isEqualTo(original);
    }

    @Test
    @DisplayName("발열 사양이 있는 제품(전기방석)은 20여 개 항목까지 왕복해도 원본과 같다")
    void 전기방석_왕복() {
        ProductProfile original = heatingPad();

        ProductProfile restored = mapper.toProfile(mapper.toRequest(original));

        assertThat(restored).isEqualTo(original);
        // 발열 항목이 실제로 실려 갔는지 — 헤어드라이어만 돌리면 전부 null 경로만 지난다.
        assertThat(restored.heating()).isEqualTo(original.heating());
    }

    @Test
    @DisplayName("보유 서류가 없어도 왕복이 깨지지 않는다")
    void 보유_서류가_없어도_왕복된다() {
        ProductProfile original = ProductProfileFixtures.hairDryer(Set.of());

        ProductProfile restored = mapper.toProfile(mapper.toRequest(original));

        assertThat(restored).isEqualTo(original);
        assertThat(restored.heldDocuments()).isEmpty();
    }

    @Test
    @DisplayName("도메인의 enum과 값 객체가 요청 DTO에서는 문자열로 나간다")
    void enum과_값객체는_문자열로_변환된다() {
        ProductProfile original = ProductProfileFixtures.hairDryer(Set.of(TEST_REPORT));

        DiagnoseRequest request = mapper.toRequest(original);

        assertThat(request.productGroup()).isEqualTo("SMALL_APPLIANCE");   // enum → String
        assertThat(request.targetUser()).isEqualTo("GENERAL");
        assertThat(request.heldDocuments()).containsExactly("TEST_REPORT"); // DocumentCode → String
        assertThat(request.materials()).containsExactlyInAnyOrder("PLASTIC", "METAL");
    }

    @Test
    @DisplayName("발열 사양이 없으면 발열 관련 필드는 모두 null로 나간다")
    void 발열_사양이_없으면_null로_나간다() {
        DiagnoseRequest request = mapper.toRequest(ProductProfileFixtures.hairDryer(Set.of()));

        assertThat(request.bodyContactType()).isNull();
        assertThat(request.controllerStatus()).isNull();
        assertThat(request.maxSurfaceTemperatureCelsius()).isNull();
        assertThat(request.autoShutOff()).isNull();
        assertThat(request.adapterCertified()).isNull();
    }

    // ── 픽스처 ────────────────────────────────────────────────────

    /**
     * 발열 항목을 되도록 서로 다른 값으로 채운 전기방석.
     *
     * <p>불리언 7개가 연달아 있어, 전부 같은 값이면 자리가 바뀌어도 왕복이 통과해 버린다.
     * true·false를 섞어 두는 것이 이 픽스처의 목적이다.
     */
    private static ProductProfile heatingPad() {
        return new ProductProfile(
                "보온용 전기방석",
                ProductGroup.ELECTRIC_HEATING_PAD,
                new ElectricalSpec(true, 220, 60, false),
                new HeatingSpec(
                        BodyContactType.DIRECT_SKIN,
                        ControllerStatus.PRESENT,
                        3,                                  // adjustmentSteps
                        45,                                 // maxSurfaceTemperatureCelsius
                        TemperatureSource.MEASURED,
                        false,                              // medicalUseClaim
                        true,                               // autoShutOff
                        30,                                 // autoShutOffMinutes
                        false,                              // overheatProtection
                        true,                               // removableCover
                        false,                              // washable
                        true,                               // separableElectricParts
                        true,                               // hasSeparateAdapter
                        false,                              // adapterExternallyAttached
                        true,                               // adapterCertified
                        AdjustmentMode.STEP,
                        false),                             // temperatureLimitDevice
                TargetUser.GENERAL,
                SalesChannel.ONLINE,
                Set.of(MaterialType.TEXTILE),
                Set.of(BIZ_LICENSE),
                ManufacturingType.SELF_MADE,
                true);                                      // modifiedModel
    }
}
