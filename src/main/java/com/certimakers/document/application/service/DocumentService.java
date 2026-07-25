package com.certimakers.document.application.service;

import com.certimakers.common.application.annotation.UseCase;
import com.certimakers.common.application.support.BlockingBridge;
import com.certimakers.common.domain.error.BusinessException;
import com.certimakers.common.domain.port.IdGenerator;
import com.certimakers.common.domain.port.TimeProvider;
import com.certimakers.document.application.port.in.DocumentUseCase;
import com.certimakers.document.application.port.out.IssuedDocumentRepositoryPort;
import com.certimakers.document.application.port.out.LoadIssuerNamePort;
import com.certimakers.document.application.port.out.RenderDocumentPdfPort;
import com.certimakers.document.application.port.out.StoreGeneratedFilePort;
import com.certimakers.document.domain.error.DocumentErrorCode;
import com.certimakers.document.domain.model.DocumentId;
import com.certimakers.document.domain.model.DocumentTemplate;
import com.certimakers.document.domain.model.FormValues;
import com.certimakers.document.domain.model.IssuedDocument;
import com.certimakers.document.domain.model.IssuerRef;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import reactor.core.publisher.Mono;

/**
 * 문서 발급 오케스트레이션: 값 검증 → PDF 렌더링 → 파일 저장 → 이력 기록.
 *
 * <p>값 검증은 {@link FormValues}가 스스로 한다. 이 서비스는 순서를 조율할 뿐이다.
 *
 * <p><b>이력을 마지막에 저장한다.</b> PDF 저장이 실패하면 이력도 남지 않아 "발급됐다는데 파일이 없는"
 * 상태가 생기지 않는다. 반대로 이력만 남고 파일이 없으면 목록에서 깨진 링크가 보인다.
 */
@UseCase
public class DocumentService implements DocumentUseCase {

    private static final int MAX_PAGE_SIZE = 50;

    private final RenderDocumentPdfPort renderPort;
    private final StoreGeneratedFilePort storePort;
    private final IssuedDocumentRepositoryPort repository;
    private final LoadIssuerNamePort loadIssuerNamePort;
    private final BlockingBridge blockingBridge;
    private final IdGenerator idGenerator;
    private final TimeProvider timeProvider;

    public DocumentService(
            RenderDocumentPdfPort renderPort,
            StoreGeneratedFilePort storePort,
            IssuedDocumentRepositoryPort repository,
            LoadIssuerNamePort loadIssuerNamePort,
            BlockingBridge blockingBridge,
            IdGenerator idGenerator,
            TimeProvider timeProvider) {
        this.renderPort = renderPort;
        this.storePort = storePort;
        this.repository = repository;
        this.loadIssuerNamePort = loadIssuerNamePort;
        this.blockingBridge = blockingBridge;
        this.idGenerator = idGenerator;
        this.timeProvider = timeProvider;
    }

    @Override
    public List<DocumentTemplate> templates() {
        return List.of(DocumentTemplate.values());
    }

    @Override
    public Mono<IssuedResult> issue(IssueCommand command) {
        DocumentTemplate template = parseTemplate(command.templateCode());
        // 값 검증은 여기서 끝난다. 이후 단계는 검증된 값만 다룬다.
        FormValues values = new FormValues(template, command.values());
        IssuerRef issuer = IssuerRef.of(command.issuerId());
        Instant issuedAt = timeProvider.now();

        // 렌더링은 CPU 작업이고 발급자 조회는 JPA다. 둘 다 이벤트 루프 밖에서 돌린다.
        return blockingBridge.mono(() -> {
                    String nickname = loadIssuerNamePort.findNickname(issuer.value()).orElse("-");
                    return renderPort.render(values, nickname, issuedAt);
                })
                .flatMap(pdf -> storePort.store(fileNameOf(template, issuedAt), pdf, command.issuerId()))
                .flatMap(fileId -> record(values, issuer, fileId, issuedAt));
    }

    private Mono<IssuedResult> record(
            FormValues values, IssuerRef issuer, Long fileId, Instant issuedAt) {

        IssuedDocument document = IssuedDocument.issue(
                DocumentId.of(idGenerator.nextId()), values, issuer, fileId, issuedAt);

        return blockingBridge.mono(() -> repository.save(document))
                .map(saved -> new IssuedResult(saved, "/api/v1/files/" + fileId));
    }

    @Override
    public Mono<List<IssuedDocument>> myDocuments(String requesterId, int page, int size) {
        IssuerRef issuer = IssuerRef.of(requesterId);
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);

        return blockingBridge.mono(() -> repository.findByIssuer(issuer, safePage, safeSize));
    }

    @Override
    public Mono<IssuedDocument> get(String documentId, String requesterId, boolean requesterIsAdmin) {
        DocumentId id = parseId(documentId);
        IssuerRef requester = IssuerRef.of(requesterId);

        return blockingBridge.mono(() -> {
            IssuedDocument document = repository.findById(id)
                    .orElseThrow(() -> new BusinessException(DocumentErrorCode.ISSUED_DOCUMENT_NOT_FOUND));
            document.requireReadableBy(requester, requesterIsAdmin);
            return document;
        });
    }

    /** 파일명에 양식 이름과 발급 시각을 넣어 사용자가 내려받은 뒤에도 구분할 수 있게 한다. */
    private String fileNameOf(DocumentTemplate template, Instant issuedAt) {
        String date = issuedAt.toString().substring(0, 10);
        return "%s_%s.pdf".formatted(template.displayName().replace(" ", ""), date);
    }

    private DocumentTemplate parseTemplate(String code) {
        if (code == null || code.isBlank()) {
            throw new BusinessException(DocumentErrorCode.TEMPLATE_NOT_FOUND);
        }
        try {
            return DocumentTemplate.valueOf(code.strip().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new BusinessException(
                    DocumentErrorCode.TEMPLATE_NOT_FOUND,
                    "'%s' 양식을 찾을 수 없습니다. 가능한 값: %s".formatted(code, availableCodes()),
                    Map.of("templateCode", code));
        }
    }

    private String availableCodes() {
        return Arrays.stream(DocumentTemplate.values())
                .map(Enum::name)
                .collect(java.util.stream.Collectors.joining(", "));
    }

    private DocumentId parseId(String rawId) {
        try {
            return DocumentId.of(Long.parseLong(rawId));
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BusinessException(DocumentErrorCode.ISSUED_DOCUMENT_NOT_FOUND);
        }
    }
}
