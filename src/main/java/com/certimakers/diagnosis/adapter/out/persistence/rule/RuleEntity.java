package com.certimakers.diagnosis.adapter.out.persistence.rule;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * {@code rule} 테이블 매핑. {@code condition}·{@code effects}는 jsonb를 문자열로 담고,
 * {@link RuleJsonCodec}이 도메인 트리로 되돌린다.
 */
@Entity
@Table(name = "rule")
public class RuleEntity {

    @Id
    private Long id;

    @ManyToOne(fetch = jakarta.persistence.FetchType.LAZY)
    @JoinColumn(name = "rule_set_id", nullable = false)
    private RuleSetEntity ruleSet;

    @Column(name = "rule_code", nullable = false)
    private String ruleCode;

    @Column(nullable = false)
    private int priority;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private String condition;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private String effects;

    @Column
    private String description;

    protected RuleEntity() {
    }

    /**
     * 새 룰을 만든다(관리자 API 경로). {@code condition}·{@code effects}는 이미 검증된 JSON 문자열이며,
     * 진단 시 {@link RuleJsonCodec}이 도메인 트리로 되돌린다. 룰셋 연관은 {@link #assignRuleSet}이 건다.
     */
    public RuleEntity(Long id, String ruleCode, int priority, String condition, String effects,
                      String description) {
        this.id = id;
        this.ruleCode = ruleCode;
        this.priority = priority;
        this.condition = condition;
        this.effects = effects;
        this.description = description;
    }

    void assignRuleSet(RuleSetEntity ruleSet) {
        this.ruleSet = ruleSet;
    }

    public String getRuleCode() {
        return ruleCode;
    }

    public String getDescription() {
        return description;
    }

    public int getPriority() {
        return priority;
    }

    public String getCondition() {
        return condition;
    }

    public String getEffects() {
        return effects;
    }
}
