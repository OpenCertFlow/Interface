package io.opencertflow.document.domain.model;

import io.opencertflow.common.domain.error.BusinessException;
import io.opencertflow.common.domain.model.AggregateRoot;
import io.opencertflow.common.domain.model.Guard;
import io.opencertflow.document.domain.error.DocumentErrorCode;
import java.time.Instant;

/**
 * 발급된 문서 한 건. 어떤 양식을, 어떤 값으로, 누가, 언제 발급했는지를 남긴다.
 *
 * <p>생성된 PDF 자체는 파일 컨텍스트에 저장하고 여기서는 {@code fileId}로 참조만 한다. 바이트를
 * 이 애그리거트에 담으면 이력 목록을 읽을 때마다 PDF 전체를 메모리에 올리게 된다.
 *
 * <p>입력값을 함께 보관하는 이유는 <b>재발급</b> 때문이다. 값이 없으면 사용자는 같은 내용을 처음부터
 * 다시 입력해야 한다.
 */
public class IssuedDocument extends AggregateRoot<DocumentId> {

    private final DocumentId id;
    private final DocumentTemplate template;
    private final FormValues values;
    private final IssuerRef issuer;
    private final Long fileId;
    private final Instant issuedAt;

    private IssuedDocument(
            DocumentId id, DocumentTemplate template, FormValues values,
            IssuerRef issuer, Long fileId, Instant issuedAt) {
        this.id = Guard.notNull(id, "id");
        this.template = Guard.notNull(template, "template");
        this.values = Guard.notNull(values, "values");
        this.issuer = Guard.notNull(issuer, "issuer");
        this.fileId = Guard.notNull(fileId, "fileId");
        this.issuedAt = Guard.notNull(issuedAt, "issuedAt");
    }

    public static IssuedDocument issue(
            DocumentId id, FormValues values, IssuerRef issuer, Long fileId, Instant issuedAt) {
        return new IssuedDocument(id, values.template(), values, issuer, fileId, issuedAt);
    }

    /** 저장된 상태에서 되살린다(영속성 재구성 전용). */
    public static IssuedDocument reconstitute(
            DocumentId id, DocumentTemplate template, FormValues values,
            IssuerRef issuer, Long fileId, Instant issuedAt) {
        return new IssuedDocument(id, template, values, issuer, fileId, issuedAt);
    }

    /**
     * 조회 권한을 확인한다. 발급자 본인 또는 관리자만 볼 수 있다.
     *
     * <p>발급 문서에는 사업자등록번호·연락처 같은 개인·사업 정보가 들어간다. 식별자를 아는 것만으로
     * 열람할 수 있으면 안 된다.
     */
    public void requireReadableBy(IssuerRef requester, boolean requesterIsAdmin) {
        if (requesterIsAdmin) {
            return;
        }
        if (!issuer.equals(requester)) {
            throw new BusinessException(DocumentErrorCode.NOT_DOCUMENT_OWNER);
        }
    }

    @Override
    public DocumentId id() {
        return id;
    }

    public DocumentTemplate template() {
        return template;
    }

    public FormValues values() {
        return values;
    }

    public IssuerRef issuer() {
        return issuer;
    }

    public Long fileId() {
        return fileId;
    }

    public Instant issuedAt() {
        return issuedAt;
    }
}
