package com.certimakers.diagnosis.adapter.out.persistence.rule;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** {@code rule_set} 테이블 매핑. 도메인 RuleSet과 별개다(ADR-0001). */
@Entity
@Table(name = "rule_set")
public class RuleSetEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private int version;

    @Column(name = "product_group", nullable = false)
    private String productGroup;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "activated_at")
    private Instant activatedAt;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "ruleSet", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<RuleEntity> rules = new ArrayList<>();

    protected RuleSetEntity() {
    }

    public UUID getId() {
        return id;
    }

    public int getVersion() {
        return version;
    }

    public String getProductGroup() {
        return productGroup;
    }

    public boolean isActive() {
        return active;
    }

    public List<RuleEntity> getRules() {
        return rules;
    }
}
