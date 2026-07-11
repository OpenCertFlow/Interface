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

    protected ProductProfileEntity() {
    }

    public ProductProfileEntity(
            String productName, String productGroup, boolean usesElectricity,
            Integer ratedVoltage, Integer powerConsumption, boolean hasBattery,
            String targetUser, String salesChannel, String materials, String heldDocuments) {
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
}
