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

    /** 마지막으로 확인한 원문 본문의 SHA-256. 아직 한 번도 못 가져왔으면 null. */
    @Column(name = "content_hash")
    private String contentHash;

    @Column(name = "content_checked_at")
    private Instant contentCheckedAt;

    /** 해시가 달라진 것을 감지한 시각. 관리자가 재검토를 마치면 비운다. */
    @Column(name = "change_detected_at")
    private Instant changeDetectedAt;

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

    public String getContentHash() {
        return contentHash;
    }

    public Instant getContentCheckedAt() {
        return contentCheckedAt;
    }

    public Instant getChangeDetectedAt() {
        return changeDetectedAt;
    }

    /** 원문을 확인한 결과를 반영한다. 해시가 달라졌으면 감지 시각을 남긴다. */
    public void recordContentCheck(String hash, Instant checkedAt) {
        boolean changed = contentHash != null && hash != null && !contentHash.equals(hash);
        this.contentHash = hash;
        this.contentCheckedAt = checkedAt;
        if (changed) {
            this.changeDetectedAt = checkedAt;
        }
    }

    /** 관리자가 재검토를 마쳤다. 변경 표시를 지운다. */
    public void clearChangeFlag() {
        this.changeDetectedAt = null;
    }
}
