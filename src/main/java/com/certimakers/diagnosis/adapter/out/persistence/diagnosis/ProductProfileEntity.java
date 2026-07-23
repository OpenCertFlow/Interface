package com.certimakers.diagnosis.adapter.out.persistence.diagnosis;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** {@code product_profile} 테이블 매핑. 진단과 1:1이며 PK를 공유한다(@MapsId). */
@Entity
@Table(name = "product_profile")
public class ProductProfileEntity {

    @Id
    @Column(name = "diagnosis_id")
    private UUID diagnosisId;

    @OneToOne
    @MapsId
    @JoinColumn(name = "diagnosis_id")
    private DiagnosisEntity diagnosis;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "product_group", nullable = false)
    private String productGroup;

    @Column(name = "uses_electricity", nullable = false)
    private boolean usesElectricity;

    @Column(name = "rated_voltage")
    private Integer ratedVoltage;

    @Column(name = "power_consumption")
    private Integer powerConsumption;

    @Column(name = "has_battery", nullable = false)
    private boolean hasBattery;

    @Column(name = "target_user", nullable = false)
    private String targetUser;

    @Column(name = "sales_channel", nullable = false)
    private String salesChannel;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private String materials;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "held_documents", nullable = false)
    private String heldDocuments;

    // ── 발열 사양. 발열 제품이 아니면 셋 다 null이다 ──
    // 세 컬럼이 함께 null이거나 함께 채워진다는 규칙은 DB CHECK 제약이 강제한다(V8).

    @Column(name = "direct_body_contact")
    private Boolean directBodyContact;

    @Column(name = "has_temperature_controller")
    private Boolean hasTemperatureController;

    /** 최고 표면온도(℃). 발열 제품이어도 측정하지 않았으면 null — "모른다"는 뜻이다. */
    @Column(name = "max_surface_temperature")
    private Integer maxSurfaceTemperature;

    protected ProductProfileEntity() {
    }

    public ProductProfileEntity(
            String productName, String productGroup, boolean usesElectricity,
            Integer ratedVoltage, Integer powerConsumption, boolean hasBattery,
            String targetUser, String salesChannel, String materials, String heldDocuments,
            Boolean directBodyContact, Boolean hasTemperatureController,
            Integer maxSurfaceTemperature) {
        this.productName = productName;
        this.productGroup = productGroup;
        this.usesElectricity = usesElectricity;
        this.ratedVoltage = ratedVoltage;
        this.powerConsumption = powerConsumption;
        this.hasBattery = hasBattery;
        this.targetUser = targetUser;
        this.salesChannel = salesChannel;
        this.materials = materials;
        this.heldDocuments = heldDocuments;
        this.directBodyContact = directBodyContact;
        this.hasTemperatureController = hasTemperatureController;
        this.maxSurfaceTemperature = maxSurfaceTemperature;
    }

    void setDiagnosis(DiagnosisEntity diagnosis) {
        this.diagnosis = diagnosis;
    }

    public String getProductName() {
        return productName;
    }

    public String getProductGroup() {
        return productGroup;
    }

    public boolean isUsesElectricity() {
        return usesElectricity;
    }

    public Integer getRatedVoltage() {
        return ratedVoltage;
    }

    public Integer getPowerConsumption() {
        return powerConsumption;
    }

    public boolean isHasBattery() {
        return hasBattery;
    }

    public String getTargetUser() {
        return targetUser;
    }

    public String getSalesChannel() {
        return salesChannel;
    }

    public String getMaterials() {
        return materials;
    }

    public String getHeldDocuments() {
        return heldDocuments;
    }

    public Boolean getDirectBodyContact() {
        return directBodyContact;
    }

    public Boolean getHasTemperatureController() {
        return hasTemperatureController;
    }

    public Integer getMaxSurfaceTemperature() {
        return maxSurfaceTemperature;
    }

    /** 발열 사양이 저장되어 있는지. 두 불리언이 모두 있어야 발열 제품으로 본다. */
    public boolean hasHeatingSpec() {
        return directBodyContact != null && hasTemperatureController != null;
    }
}
