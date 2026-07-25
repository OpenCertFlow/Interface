package com.certimakers.diagnosis.adapter.out.persistence.document;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;

/** {@code official_document} 매핑. source_url은 NOT NULL — 출처 없는 문서는 근거가 아니다(불변식 6). */
@Entity
@Table(name = "official_document")
public class OfficialDocumentEntity {

    @Id
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String issuer;

    @Column(name = "published_at")
    private LocalDate publishedAt;

    @Column(name = "verified_at")
    private LocalDate verifiedAt;

    @Column(name = "product_group", nullable = false)
    private String productGroup;

    @Column(name = "certification_type")
    private String certificationType;

    @Column(name = "scheme_name")
    private String schemeName;

    @Column(name = "source_url", nullable = false)
    private String sourceUrl;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    protected OfficialDocumentEntity() {
    }

    public OfficialDocumentEntity(Long id, String title, String issuer, LocalDate publishedAt,
                                  LocalDate verifiedAt, String productGroup, String certificationType,
                                  String schemeName, String sourceUrl) {
        this.id = id;
        this.title = title;
        this.issuer = issuer;
        this.publishedAt = publishedAt;
        this.verifiedAt = verifiedAt;
        this.productGroup = productGroup;
        this.certificationType = certificationType;
        this.schemeName = schemeName;
        this.sourceUrl = sourceUrl;
    }

    public void update(String title, String issuer, LocalDate publishedAt, LocalDate verifiedAt,
                       String productGroup, String certificationType, String schemeName,
                       String sourceUrl) {
        this.title = title;
        this.issuer = issuer;
        this.publishedAt = publishedAt;
        this.verifiedAt = verifiedAt;
        this.productGroup = productGroup;
        this.certificationType = certificationType;
        this.schemeName = schemeName;
        this.sourceUrl = sourceUrl;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getIssuer() {
        return issuer;
    }

    public LocalDate getPublishedAt() {
        return publishedAt;
    }

    public LocalDate getVerifiedAt() {
        return verifiedAt;
    }

    public String getProductGroup() {
        return productGroup;
    }

    public String getCertificationType() {
        return certificationType;
    }

    public String getSchemeName() {
        return schemeName;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
