package com.certimakers.diagnosis.adapter.out.persistence.rule;

import com.certimakers.common.domain.error.BusinessException;
import com.certimakers.common.domain.error.CommonErrorCode;
import com.certimakers.diagnosis.domain.model.AdjustmentMode;
import com.certimakers.diagnosis.domain.model.BodyContactType;
import com.certimakers.diagnosis.domain.model.CertificationType;
import com.certimakers.diagnosis.domain.model.ControllerStatus;
import com.certimakers.diagnosis.domain.model.DocumentCode;
import com.certimakers.diagnosis.domain.model.ExpertReviewReason;
import com.certimakers.diagnosis.domain.model.MaterialType;
import com.certimakers.diagnosis.domain.model.ProductGroup;
import com.certimakers.diagnosis.domain.model.Requirement;
import com.certimakers.diagnosis.domain.model.SalesChannel;
import com.certimakers.diagnosis.domain.model.SchemeCode;
import com.certimakers.diagnosis.domain.model.TargetUser;
import com.certimakers.diagnosis.domain.model.TemperatureSource;
import com.certimakers.diagnosis.domain.rule.AddCandidate;
import com.certimakers.diagnosis.domain.rule.AddLabelingCheck;
import com.certimakers.diagnosis.domain.rule.AllOf;
import com.certimakers.diagnosis.domain.rule.AnyOf;
import com.certimakers.diagnosis.domain.rule.Attribute;
import com.certimakers.diagnosis.domain.rule.AttributeMatch;
import com.certimakers.diagnosis.domain.rule.Condition;
import com.certimakers.diagnosis.domain.rule.Effect;
import com.certimakers.diagnosis.domain.rule.FlagExpertReview;
import com.certimakers.diagnosis.domain.rule.Not;
import com.certimakers.diagnosis.domain.rule.Operator;
import com.certimakers.diagnosis.domain.rule.RequireDocument;
import com.certimakers.diagnosis.domain.rule.ValueKind;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * jsonb에 저장된 룰의 {@code condition}·{@code effects}를 도메인 sealed 트리로 되돌린다.
 *
 * <p>이 코덱은 <b>읽기 전용</b>이다. 룰은 {@code R__seed_rules.sql}이 손으로 쓴 JSON으로 주입하므로
 * 쓰기 경로가 필요 없다. 도메인이 Jackson을 참조하지 않도록(ArchUnit) 타입 변환 지식은 여기 모은다.
 *
 * <p>JSON 형식은 {@code docs}의 룰 모델을 따른다:
 * <pre>
 * condition: {"type":"allOf","conditions":[ {"type":"attr","attribute":"RATED_VOLTAGE","operator":"GT","value":50} ]}
 * effects:   [ {"type":"addCandidate","schemeCode":"...","certificationType":"SAFETY_CONFIRM"} ]
 * </pre>
 */
public class RuleJsonCodec {

    private final ObjectMapper mapper;

    public RuleJsonCodec(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public Condition parseCondition(String json) {
        return parseCondition(readTree(json));
    }

    public List<Effect> parseEffects(String json) {
        JsonNode array = readTree(json);
        if (!array.isArray()) {
            throw malformed("effects는 배열이어야 합니다.");
        }
        List<Effect> effects = new ArrayList<>();
        array.forEach(node -> effects.add(parseEffect(node)));
        return effects;
    }

    // ── condition ────────────────────────────────────────────────

    private Condition parseCondition(JsonNode node) {
        String type = requiredText(node, "type");
        return switch (type) {
            case "allOf" -> new AllOf(parseConditionList(node));
            case "anyOf" -> new AnyOf(parseConditionList(node));
            case "not" -> new Not(parseCondition(node.get("condition")));
            case "attr" -> parseAttributeMatch(node);
            default -> throw malformed("알 수 없는 condition type: " + type);
        };
    }

    private List<Condition> parseConditionList(JsonNode node) {
        JsonNode conditions = node.get("conditions");
        if (conditions == null || !conditions.isArray()) {
            throw malformed("%s에 conditions 배열이 필요합니다.".formatted(node.path("type").asText()));
        }
        List<Condition> parsed = new ArrayList<>();
        conditions.forEach(child -> parsed.add(parseCondition(child)));
        return parsed;
    }

    private AttributeMatch parseAttributeMatch(JsonNode node) {
        Attribute attribute = enumValue(Attribute.class, requiredText(node, "attribute"));
        Operator operator = enumValue(Operator.class, requiredText(node, "operator"));
        Object value = coerceValue(attribute, node.get("value"));
        return new AttributeMatch(attribute, operator, value);
    }

    /** JSON 값을 속성의 {@link ValueKind}에 맞는 타입으로 되돌린다. 배열이면 각 원소를 변환한 집합. */
    private Object coerceValue(Attribute attribute, JsonNode valueNode) {
        if (valueNode == null || valueNode.isNull()) {
            return null; // "전압 EQ null"처럼 값 부재를 검사하는 룰
        }
        if (valueNode.isArray()) {
            Set<Object> values = new LinkedHashSet<>();
            valueNode.forEach(element -> values.add(coerceScalar(attribute.valueKind(), element)));
            return values;
        }
        return coerceScalar(attribute.valueKind(), valueNode);
    }

    private Object coerceScalar(ValueKind kind, JsonNode node) {
        return switch (kind) {
            case BOOLEAN -> node.asBoolean();
            case INTEGER -> node.asInt();
            case TARGET_USER -> enumValue(TargetUser.class, node.asText());
            case SALES_CHANNEL -> enumValue(SalesChannel.class, node.asText());
            case PRODUCT_GROUP -> enumValue(ProductGroup.class, node.asText());
            case MATERIAL -> enumValue(MaterialType.class, node.asText());
            case DOCUMENT_CODE -> DocumentCode.of(node.asText());
            case BODY_CONTACT_TYPE -> enumValue(BodyContactType.class, node.asText());
            case CONTROLLER_STATUS -> enumValue(ControllerStatus.class, node.asText());
            case TEMPERATURE_SOURCE -> enumValue(TemperatureSource.class, node.asText());
            case ADJUSTMENT_MODE -> enumValue(AdjustmentMode.class, node.asText());
        };
    }

    // ── effects ──────────────────────────────────────────────────

    private Effect parseEffect(JsonNode node) {
        String type = requiredText(node, "type");
        return switch (type) {
            case "addCandidate" -> new AddCandidate(
                    SchemeCode.of(requiredText(node, "schemeCode")),
                    enumValue(CertificationType.class, requiredText(node, "certificationType")));
            case "requireDocument" -> new RequireDocument(
                    DocumentCode.of(requiredText(node, "documentCode")),
                    enumValue(Requirement.class, requiredText(node, "requirement")));
            case "addLabelingCheck" -> new AddLabelingCheck(requiredText(node, "label"));
            case "flagExpertReview" -> new FlagExpertReview(
                    requiredText(node, "question"),
                    enumValue(ExpertReviewReason.class, requiredText(node, "reason")));
            default -> throw malformed("알 수 없는 effect type: " + type);
        };
    }

    // ── helpers ──────────────────────────────────────────────────

    private JsonNode readTree(String json) {
        try {
            return mapper.readTree(json);
        } catch (Exception e) {
            throw new BusinessException(
                    CommonErrorCode.INTERNAL_ERROR, "룰 JSON 파싱 실패", java.util.Map.of(), e);
        }
    }

    private String requiredText(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull() || value.asText().isBlank()) {
            throw malformed("필수 필드 누락: " + field);
        }
        return value.asText();
    }

    private <E extends Enum<E>> E enumValue(Class<E> type, String raw) {
        try {
            return Enum.valueOf(type, raw);
        } catch (IllegalArgumentException e) {
            throw malformed("%s에 없는 값: %s".formatted(type.getSimpleName(), raw));
        }
    }

    private BusinessException malformed(String message) {
        return new BusinessException(CommonErrorCode.INTERNAL_ERROR, "룰 정의 오류 — " + message);
    }
}
