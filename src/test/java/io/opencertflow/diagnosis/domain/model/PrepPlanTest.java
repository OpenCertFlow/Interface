package io.opencertflow.diagnosis.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.opencertflow.common.domain.error.BusinessException;
import io.opencertflow.support.TestIds;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 인증 준비 계획(F-APP-049) 도메인. 진행률 산정과 "목록에 없는 서류는 못 만진다"는 불변식을
 * 검증한다. 목 없이 순수 자바로 돌아간다.
 */
class PrepPlanTest {

    private static final Instant CREATED_AT = Instant.parse("2026-08-01T00:00:00Z");
    private static final Instant CHECKED_AT = Instant.parse("2026-08-02T00:00:00Z");

    private static final DocumentCode TEST_REPORT = DocumentCode.of("TEST_REPORT");
    private static final DocumentCode BIZ_LICENSE = DocumentCode.of("BIZ_LICENSE");
    private static final DocumentCode CIRCUIT_DIAGRAM = DocumentCode.of("CIRCUIT_DIAGRAM");

    private PrepPlan plan(List<DocumentCode> missing) {
        return PrepPlan.from(
                PrepPlanId.of(TestIds.next()),
                "user-7",
                DiagnosisId.of(TestIds.next()),
                missing,
                CREATED_AT);
    }

    @Test
    @DisplayName("만들면 모든 항목이 미완료이고 진행률은 0이다")
    void 새_목록은_모두_미완료다() {
        PrepPlan plan = plan(List.of(TEST_REPORT, BIZ_LICENSE, CIRCUIT_DIAGRAM));

        assertThat(plan.items()).extracting(PrepItem::done).containsOnly(false);
        assertThat(plan.completed()).isZero();
        assertThat(plan.total()).isEqualTo(3);
        assertThat(plan.progress()).isZero();
        assertThat(plan.hasItems()).isTrue();
    }

    @Test
    @DisplayName("받은 순서를 그대로 보존한다 — 표시 순서는 진단의 보완 우선순위다")
    void 받은_순서를_보존한다() {
        // 알파벳순이라면 BIZ_LICENSE가 먼저 오지만, 진단이 정한 순서를 다시 정렬하지 않는다.
        PrepPlan plan = plan(List.of(TEST_REPORT, BIZ_LICENSE, CIRCUIT_DIAGRAM));

        assertThat(plan.items()).extracting(item -> item.documentCode().value())
                .containsExactly("TEST_REPORT", "BIZ_LICENSE", "CIRCUIT_DIAGRAM");
    }

    @Test
    @DisplayName("체크하면 완료 건수와 진행률이 오른다")
    void 체크하면_진행률이_오른다() {
        PrepPlan plan = plan(List.of(TEST_REPORT, BIZ_LICENSE, CIRCUIT_DIAGRAM));

        plan.check(TEST_REPORT, true, CHECKED_AT);

        assertThat(plan.completed()).isEqualTo(1);
        assertThat(plan.progress()).isEqualTo(33);   // round(1/3 * 100)
        assertThat(plan.updatedAt()).isEqualTo(CHECKED_AT);
    }

    @Test
    @DisplayName("체크를 해제하면 진행률이 다시 내려간다")
    void 체크를_해제하면_진행률이_내려간다() {
        PrepPlan plan = plan(List.of(TEST_REPORT, BIZ_LICENSE));
        plan.check(TEST_REPORT, true, CHECKED_AT);

        plan.check(TEST_REPORT, false, CHECKED_AT);

        assertThat(plan.completed()).isZero();
        assertThat(plan.progress()).isZero();
    }

    @Test
    @DisplayName("전부 체크하면 100%가 된다")
    void 전부_체크하면_100이다() {
        PrepPlan plan = plan(List.of(TEST_REPORT, BIZ_LICENSE));

        plan.check(TEST_REPORT, true, CHECKED_AT);
        plan.check(BIZ_LICENSE, true, CHECKED_AT);

        assertThat(plan.progress()).isEqualTo(100);
    }

    @Test
    @DisplayName("목록에 없는 서류 코드는 거부한다 — 임의 코드로 목록을 늘리지 못한다")
    void 목록에_없는_코드는_거부한다() {
        PrepPlan plan = plan(List.of(TEST_REPORT));

        assertThatThrownBy(() -> plan.check(BIZ_LICENSE, true, CHECKED_AT))
                .isInstanceOf(BusinessException.class);
        assertThat(plan.total()).isEqualTo(1);   // 늘어나지 않았다
    }

    @Test
    @DisplayName("준비할 항목이 없으면 hasItems가 false다 — 진행률 0을 '아무것도 안 했다'로 읽으면 안 된다")
    void 항목이_없으면_산정_불가다() {
        PrepPlan plan = plan(List.of());

        assertThat(plan.hasItems()).isFalse();
        assertThat(plan.total()).isZero();
        assertThat(plan.progress()).isZero();   // 0으로 나누지 않는다
    }

    @Test
    @DisplayName("소유자 본인만 자기 목록으로 인정된다")
    void 소유자를_판별한다() {
        PrepPlan plan = plan(List.of(TEST_REPORT));

        assertThat(plan.isOwnedBy("user-7")).isTrue();
        assertThat(plan.isOwnedBy("user-9")).isFalse();
    }

    @Test
    @DisplayName("항목 목록은 불변 뷰다 — 밖에서 직접 늘리거나 지울 수 없다")
    void 항목_목록은_불변이다() {
        PrepPlan plan = plan(List.of(TEST_REPORT));

        assertThatThrownBy(() -> plan.items().add(PrepItem.of(BIZ_LICENSE)))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("id가 같으면 같은 애그리거트다 — 체크 상태가 달라도 같다")
    void 동일성은_id로_판단한다() {
        PrepPlanId id = PrepPlanId.of(TestIds.next());
        DiagnosisId diagnosisId = DiagnosisId.of(TestIds.next());
        PrepPlan one = PrepPlan.from(id, "user-7", diagnosisId, List.of(TEST_REPORT), CREATED_AT);
        PrepPlan other = PrepPlan.from(id, "user-7", diagnosisId, List.of(TEST_REPORT), CREATED_AT);
        other.check(TEST_REPORT, true, CHECKED_AT);

        assertThat(one).isEqualTo(other);
    }
}
