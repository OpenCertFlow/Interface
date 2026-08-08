package io.opencertflow.document.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * {@code issued_document} 테이블 매핑.
 *
 * <p>입력값은 JSON 문자열로 담는다. 양식마다 항목이 다르므로 컬럼으로 펴면 양식을 추가할 때마다
 * 스키마가 바뀌어야 한다. 값은 재발급 시 다시 채워 주는 용도로만 읽으므로 JSON으로 충분하다.
 */
@Entity
@Table(name = "issued_document")
public class IssuedDocumentEntity {

    @Id
    private Long id;

    @Column(name = "template_code", nullable = false)
    private String templateCode;

    @Column(name = "values_json", nullable = false, columnDefinition = "TEXT")
    private String valuesJson;

    @Column(name = "issuer_id", nullable = false)
    private Long issuerId;

    @Column(name = "file_id", nullable = false)
    private Long fileId;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    protected IssuedDocumentEntity() {
    }

    public IssuedDocumentEntity(
            Long id, String templateCode, String valuesJson,
            Long issuerId, Long fileId, Instant issuedAt) {
        this.id = id;
        this.templateCode = templateCode;
        this.valuesJson = valuesJson;
        this.issuerId = issuerId;
        this.fileId = fileId;
        this.issuedAt = issuedAt;
    }

    public Long getId() {
        return id;
    }

    public String getTemplateCode() {
        return templateCode;
    }

    public String getValuesJson() {
        return valuesJson;
    }

    public Long getIssuerId() {
        return issuerId;
    }

    public Long getFileId() {
        return fileId;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }
}
