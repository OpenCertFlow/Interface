package io.opencertflow.diagnosis.adapter.out.persistence.schema;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * {@code product_group_question_override} 매핑. 입력 항목의 프레젠테이션 오버라이드다.
 *
 * <p>NULL인 컬럼은 "오버라이드 없음 → enum 기본값 사용"을 뜻한다. {@code active}만 NOT NULL이며,
 * false면 그 항목을 화면에서 숨긴다.
 */
@Entity
@Table(name = "product_group_question_override")
public class ProductGroupQuestionOverrideEntity {

    @Id
    private Long id;

    @Column(name = "product_group", nullable = false)
    private String productGroup;

    @Column(nullable = false)
    private String code;

    @Column
    private String label;

    @Column(name = "help_text")
    private String helpText;

    @Column
    private Boolean required;

    @Column(name = "display_order")
    private Integer displayOrder;

    @Column(nullable = false)
    private boolean active = true;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "options_json")
    private String optionsJson;

    protected ProductGroupQuestionOverrideEntity() {
    }

    public ProductGroupQuestionOverrideEntity(Long id, String productGroup, String code) {
        this.id = id;
        this.productGroup = productGroup;
        this.code = code;
    }

    public void apply(String label, String helpText, Boolean required, Integer displayOrder,
                      boolean active, String optionsJson) {
        this.label = label;
        this.helpText = helpText;
        this.required = required;
        this.displayOrder = displayOrder;
        this.active = active;
        this.optionsJson = optionsJson;
    }

    public Long getId() {
        return id;
    }

    public String getProductGroup() {
        return productGroup;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public String getHelpText() {
        return helpText;
    }

    public Boolean getRequired() {
        return required;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public boolean isActive() {
        return active;
    }

    public String getOptionsJson() {
        return optionsJson;
    }
}
