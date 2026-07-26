package com.certimakers.diagnosis.adapter.out.persistence.diagnosis;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** {@code product_profile} 테이블 매핑. 진단과 1:1이며 PK를 공유한다(@MapsId). */
@Entity
@Table(name = "product_profile")
public class ProductProfileEntity {

    @Id
    @Column(name = "diagnosis_id")
    private Long diagnosisId;

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

    // ── 발열 사양(F-APP-014~018). 발열 제품이 아니면 모두 null이다 ──
    // 값의 짝 규칙(온도출처↔표면온도, 조절기↔단계 등)은 도메인 HeatingSpec이 강제한다.

    @Column(name = "body_contact_type")
    private String bodyContactType;

    @Column(name = "controller_status")
    private String controllerStatus;

    @Column(name = "adjustment_steps")
    private Integer adjustmentSteps;

    @Column(name = "adjustment_mode")
    private String adjustmentMode;

    /** 최고 표면온도(℃). 출처가 모름이면 null. */
    @Column(name = "max_surface_temperature")
    private Integer maxSurfaceTemperature;

    @Column(name = "temperature_source")
    private String temperatureSource;

    @Column(name = "medical_use_claim")
    private Boolean medicalUseClaim;

    @Column(name = "auto_shut_off")
    private Boolean autoShutOff;

    @Column(name = "auto_shut_off_minutes")
    private Integer autoShutOffMinutes;

    @Column(name = "overheat_protection")
    private Boolean overheatProtection;

    @Column(name = "temperature_limit_device")
    private Boolean temperatureLimitDevice;

    @Column(name = "removable_cover")
    private Boolean removableCover;

    @Column(name = "washable")
    private Boolean washable;

    @Column(name = "separable_electric_parts")
    private Boolean separableElectricParts;

    @Column(name = "has_separate_adapter")
    private Boolean hasSeparateAdapter;

    @Column(name = "adapter_externally_attached")
    private Boolean adapterExternallyAttached;

    @Column(name = "adapter_certified")
    private Boolean adapterCertified;

    protected ProductProfileEntity() {
    }

    public ProductProfileEntity(
            String productName, String productGroup, boolean usesElectricity,
            Integer ratedVoltage, Integer powerConsumption, boolean hasBattery,
            String targetUser, String salesChannel, String materials, String heldDocuments,
            String bodyContactType, String controllerStatus, Integer adjustmentSteps,
            Integer maxSurfaceTemperature, String temperatureSource,
            Boolean medicalUseClaim, Boolean autoShutOff, Integer autoShutOffMinutes,
            Boolean overheatProtection, Boolean removableCover, Boolean washable,
            Boolean separableElectricParts, Boolean hasSeparateAdapter,
            Boolean adapterExternallyAttached, Boolean adapterCertified,
            String adjustmentMode, Boolean temperatureLimitDevice) {
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
        this.bodyContactType = bodyContactType;
        this.controllerStatus = controllerStatus;
        this.adjustmentSteps = adjustmentSteps;
        this.maxSurfaceTemperature = maxSurfaceTemperature;
        this.temperatureSource = temperatureSource;
        this.medicalUseClaim = medicalUseClaim;
        this.autoShutOff = autoShutOff;
        this.autoShutOffMinutes = autoShutOffMinutes;
        this.overheatProtection = overheatProtection;
        this.removableCover = removableCover;
        this.washable = washable;
        this.separableElectricParts = separableElectricParts;
        this.hasSeparateAdapter = hasSeparateAdapter;
        this.adapterExternallyAttached = adapterExternallyAttached;
        this.adapterCertified = adapterCertified;
        this.adjustmentMode = adjustmentMode;
        this.temperatureLimitDevice = temperatureLimitDevice;
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

    public String getBodyContactType() {
        return bodyContactType;
    }

    public String getControllerStatus() {
        return controllerStatus;
    }

    public Integer getAdjustmentSteps() {
        return adjustmentSteps;
    }

    public String getAdjustmentMode() {
        return adjustmentMode;
    }

    public Boolean getTemperatureLimitDevice() {
        return temperatureLimitDevice;
    }

    public Integer getMaxSurfaceTemperature() {
        return maxSurfaceTemperature;
    }

    public String getTemperatureSource() {
        return temperatureSource;
    }

    public Boolean getMedicalUseClaim() {
        return medicalUseClaim;
    }

    public Boolean getAutoShutOff() {
        return autoShutOff;
    }

    public Integer getAutoShutOffMinutes() {
        return autoShutOffMinutes;
    }

    public Boolean getOverheatProtection() {
        return overheatProtection;
    }

    public Boolean getRemovableCover() {
        return removableCover;
    }

    public Boolean getWashable() {
        return washable;
    }

    public Boolean getSeparableElectricParts() {
        return separableElectricParts;
    }

    public Boolean getHasSeparateAdapter() {
        return hasSeparateAdapter;
    }

    public Boolean getAdapterExternallyAttached() {
        return adapterExternallyAttached;
    }

    public Boolean getAdapterCertified() {
        return adapterCertified;
    }

    /** 발열 사양이 저장되어 있는지. 신체접촉 방식이 있으면 발열 제품으로 본다. */
    public boolean hasHeatingSpec() {
        return bodyContactType != null;
    }
}
