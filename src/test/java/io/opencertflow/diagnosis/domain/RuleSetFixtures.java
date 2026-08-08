package io.opencertflow.diagnosis.domain;

import static io.opencertflow.diagnosis.domain.rule.Attribute.HAS_BATTERY;
import static io.opencertflow.diagnosis.domain.rule.Attribute.RATED_VOLTAGE;
import static io.opencertflow.diagnosis.domain.rule.Attribute.TARGET_USER;
import static io.opencertflow.diagnosis.domain.rule.Attribute.USES_ELECTRICITY;
import static io.opencertflow.diagnosis.domain.rule.Operator.EQ;
import static io.opencertflow.diagnosis.domain.rule.Operator.GT;

import io.opencertflow.diagnosis.domain.model.CertificationType;
import io.opencertflow.diagnosis.domain.model.DocumentCode;
import io.opencertflow.diagnosis.domain.model.ProductGroup;
import io.opencertflow.diagnosis.domain.model.Requirement;
import io.opencertflow.diagnosis.domain.model.SchemeCode;
import io.opencertflow.diagnosis.domain.model.TargetUser;
import io.opencertflow.diagnosis.domain.rule.AddCandidate;
import io.opencertflow.diagnosis.domain.rule.AddLabelingCheck;
import io.opencertflow.diagnosis.domain.rule.AllOf;
import io.opencertflow.diagnosis.domain.rule.AttributeMatch;
import io.opencertflow.diagnosis.domain.rule.FlagExpertReview;
import io.opencertflow.diagnosis.domain.rule.Not;
import io.opencertflow.diagnosis.domain.rule.RequireDocument;
import io.opencertflow.diagnosis.domain.rule.Rule;
import io.opencertflow.diagnosis.domain.rule.RuleCode;
import io.opencertflow.diagnosis.domain.rule.RuleSet;
import io.opencertflow.diagnosis.domain.rule.RuleSetVersion;
import io.opencertflow.diagnosis.domain.model.ExpertReviewReason;
import java.util.List;

/**
 * 테스트용 소형가전 룰셋. 실제 KC 제도를 단순화한 데모 규칙이며, 본선 룰셋(R__seed_rules.sql)의
 * 프로토타입이다. 전체 진단 흐름의 작동을 검증하는 것이 목적이다.
 */
public final class RuleSetFixtures {

    public static final DocumentCode BIZ_LICENSE = DocumentCode.of("BIZ_LICENSE");
    public static final DocumentCode TEST_REPORT = DocumentCode.of("TEST_REPORT");
    public static final DocumentCode CIRCUIT_DIAGRAM = DocumentCode.of("CIRCUIT_DIAGRAM");
    public static final DocumentCode PARTS_LIST = DocumentCode.of("PARTS_LIST");
    public static final DocumentCode SAFETY_LABEL_SAMPLE = DocumentCode.of("SAFETY_LABEL_SAMPLE");

    public static final SchemeCode SAFETY_CONFIRM_ELECTRIC =
            SchemeCode.of("KC_SAFETY_CONFIRM_ELECTRIC");
    public static final SchemeCode CHILD_SAFETY_CERT = SchemeCode.of("KC_CHILD_SAFETY_CERT");

    private RuleSetFixtures() {
    }

    /**
     * 소형가전 데모 룰셋 v1.
     *
     * <ul>
     *   <li>R-SA-001: 전기 사용 + 정격전압 &gt; 50V → 안전확인 후보 + 필수 서류들</li>
     *   <li>R-SA-002: 전기 사용 + 정격전압 &gt; 50V → 안전표시 라벨링 확인</li>
     *   <li>R-SA-010: 어린이용 → 어린이제품 안전인증 후보</li>
     *   <li>R-SA-090: 전기 사용인데 정격전압 정보 없음 → 전문가 확인(AMBIGUOUS_CONDITION)</li>
     * </ul>
     */
    public static RuleSet smallApplianceV1() {
        Rule electricSafetyConfirm = new Rule(
                RuleCode.of("R-SA-001"),
                10,
                AllOf.of(
                        AttributeMatch.of(USES_ELECTRICITY, EQ, true),
                        AttributeMatch.of(RATED_VOLTAGE, GT, 50)),
                List.of(
                        new AddCandidate(SAFETY_CONFIRM_ELECTRIC, CertificationType.SAFETY_CONFIRM),
                        new RequireDocument(BIZ_LICENSE, Requirement.REQUIRED),
                        new RequireDocument(TEST_REPORT, Requirement.REQUIRED),
                        new RequireDocument(CIRCUIT_DIAGRAM, Requirement.RECOMMENDED),
                        new RequireDocument(PARTS_LIST, Requirement.RECOMMENDED)));

        Rule electricLabeling = new Rule(
                RuleCode.of("R-SA-002"),
                20,
                AllOf.of(
                        AttributeMatch.of(USES_ELECTRICITY, EQ, true),
                        AttributeMatch.of(RATED_VOLTAGE, GT, 50)),
                List.of(
                        new AddLabelingCheck("KC 마크 및 안전확인 표시"),
                        new AddLabelingCheck("정격전압·소비전력 표시"),
                        new RequireDocument(SAFETY_LABEL_SAMPLE, Requirement.REQUIRED)));

        Rule childProduct = new Rule(
                RuleCode.of("R-SA-010"),
                10,
                AttributeMatch.of(TARGET_USER, EQ, TargetUser.CHILD),
                List.of(new AddCandidate(CHILD_SAFETY_CERT, CertificationType.SAFETY_CERT)));

        // 전기는 쓰는데 전압 정보가 없으면 판단할 수 없다. 후보를 지목하는 대신 전문가 확인으로 보낸다.
        Rule ambiguousVoltage = new Rule(
                RuleCode.of("R-SA-090"),
                90,
                AllOf.of(
                        AttributeMatch.of(USES_ELECTRICITY, EQ, true),
                        AttributeMatch.of(RATED_VOLTAGE, EQ, null),
                        new Not(AttributeMatch.of(HAS_BATTERY, EQ, true))),
                List.of(new FlagExpertReview(
                        "정격전압 정보가 없어 안전확인 대상 여부를 판단할 수 없습니다. 정격전압을 확인해 주세요.",
                        ExpertReviewReason.AMBIGUOUS_CONDITION)));

        return new RuleSet(
                RuleSetVersion.of(1),
                ProductGroup.SMALL_APPLIANCE,
                List.of(electricSafetyConfirm, electricLabeling, childProduct, ambiguousVoltage));
    }
}
