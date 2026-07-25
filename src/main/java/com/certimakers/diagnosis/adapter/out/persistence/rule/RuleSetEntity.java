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

/** {@code rule_set} 테이블 매핑. 도메인 RuleSet과 별개다(ADR-0001). */
@Entity
@Table(name = "rule_set")
public class RuleSetEntity {

    @Id
    private Long id;

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

    /**
     * 새 비활성 룰셋 초안을 만든다(관리자 API 경로). 활성화는 {@link #activate(Instant)}로 별도로 한다 —
     * 저장과 배포를 분리해, 검증되지 않은 룰셋이 곧바로 진단에 쓰이지 않게 한다.
     */
    public RuleSetEntity(Long id, int version, String productGroup) {
        this.id = id;
        this.version = version;
        this.productGroup = productGroup;
        this.active = false;
    }

    /** 룰을 이 룰셋에 추가하며 양방향 연관을 맞춘다. */
    public void addRule(RuleEntity rule) {
        rule.assignRuleSet(this);
        this.rules.add(rule);
    }

    /** 활성화(배포). 부분 유니크 인덱스가 제품군당 하나만 허용하므로, 호출 전 기존 활성본을 꺼야 한다. */
    public void activate(Instant when) {
        this.active = true;
        this.activatedAt = when;
    }

    /** 비활성화. 새 버전을 배포할 때 기존 활성본에 적용한다. */
    public void deactivate() {
        this.active = false;
        this.activatedAt = null;
    }

    public Long getId() {
        return id;
    }

    public Instant getActivatedAt() {
        return activatedAt;
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
