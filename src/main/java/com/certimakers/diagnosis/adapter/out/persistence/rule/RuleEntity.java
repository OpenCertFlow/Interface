package com.certimakers.diagnosis.adapter.out.persistence.rule;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
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
    private UUID id;

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

    public String getRuleCode() {
        return ruleCode;
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
