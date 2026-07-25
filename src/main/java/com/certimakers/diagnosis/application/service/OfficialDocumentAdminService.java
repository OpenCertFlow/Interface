package com.certimakers.diagnosis.application.service;

import com.certimakers.common.application.annotation.UseCase;
import com.certimakers.common.application.support.BlockingBridge;
import com.certimakers.common.domain.error.BusinessException;
import com.certimakers.diagnosis.application.port.in.ManageOfficialDocumentUseCase;
import com.certimakers.diagnosis.application.port.out.OfficialDocumentAdminPort;
import com.certimakers.diagnosis.application.port.out.OfficialDocumentAdminPort.DocumentData;
import com.certimakers.diagnosis.application.port.out.OfficialDocumentAdminPort.DocumentRow;
import com.certimakers.diagnosis.domain.model.CertificationType;
import com.certimakers.diagnosis.domain.model.ProductGroup;
import java.util.List;
import reactor.core.publisher.Mono;

/** 공식 문서 메타데이터 등록·수정·조회. 본문 색인(Vector DB)은 AI워커 소관이라 여기선 메타데이터만 다룬다. */
@UseCase
public class OfficialDocumentAdminService implements ManageOfficialDocumentUseCase {

    private final OfficialDocumentAdminPort documentPort;
    private final BlockingBridge blockingBridge;

    public OfficialDocumentAdminService(
            OfficialDocumentAdminPort documentPort, BlockingBridge blockingBridge) {
        this.documentPort = documentPort;
        this.blockingBridge = blockingBridge;
    }

    @Override
    public Mono<List<DocumentView>> list() {
        return blockingBridge.mono(() -> documentPort.findAll().stream()
                .map(this::toView)
                .toList());
    }

    @Override
    public Mono<DocumentView> get(Long id) {
        return blockingBridge.mono(() -> documentPort.findById(id).orElse(null))
                .switchIfEmpty(Mono.error(BusinessException.invalid("문서를 찾을 수 없습니다: " + id)))
                .map(this::toView);
    }

    @Override
    public Mono<Long> register(DocumentCommand command) {
        return Mono.fromSupplier(() -> validated(command))
                .flatMap(data -> blockingBridge.mono(() -> documentPort.register(data)));
    }

    @Override
    public Mono<Void> update(Long id, DocumentCommand command) {
        return Mono.fromSupplier(() -> validated(command))
                .flatMap(data -> blockingBridge.mono(() -> documentPort.update(id, data)))
                .flatMap(found -> Boolean.TRUE.equals(found)
                        ? Mono.empty()
                        : Mono.error(BusinessException.invalid("문서를 찾을 수 없습니다: " + id)));
    }

    private DocumentData validated(DocumentCommand command) {
        requireText(command.title(), "제목");
        requireText(command.issuer(), "발행 기관");
        requireSourceUrl(command.sourceUrl());
        String productGroup = requireProductGroup(command.productGroup());
        String certificationType = normalizeCertificationType(command.certificationType());
        return new DocumentData(
                command.title().strip(), command.issuer().strip(),
                command.publishedAt(), command.verifiedAt(), productGroup, certificationType,
                blankToNull(command.schemeName()), command.sourceUrl().strip());
    }

    private void requireText(String value, String label) {
        if (value == null || value.isBlank()) {
            throw BusinessException.invalid("%s은(는) 필수입니다.".formatted(label));
        }
    }

    // 출처 URL은 필수 + http(s)만 허용 — 원문으로 되짚을 수 없는 문서는 근거가 아니다(불변식 6).
    private void requireSourceUrl(String sourceUrl) {
        if (sourceUrl == null || sourceUrl.isBlank()) {
            throw BusinessException.invalid("출처 URL은 필수입니다. 원문으로 되짚을 수 없는 문서는 근거가 아닙니다.");
        }
        String trimmed = sourceUrl.strip();
        if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            throw BusinessException.invalid("출처 URL은 http:// 또는 https:// 로 시작해야 합니다.");
        }
    }

    private String requireProductGroup(String raw) {
        if (raw == null || raw.isBlank()) {
            throw BusinessException.invalid("제품군은 필수입니다.");
        }
        try {
            return ProductGroup.valueOf(raw).name();
        } catch (IllegalArgumentException e) {
            throw BusinessException.invalid("제품군 값이 올바르지 않습니다: " + raw);
        }
    }

    private String normalizeCertificationType(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return CertificationType.valueOf(raw).name();
        } catch (IllegalArgumentException e) {
            throw BusinessException.invalid("인증 유형 값이 올바르지 않습니다: " + raw);
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    private DocumentView toView(DocumentRow row) {
        return new DocumentView(
                row.id(), row.title(), row.issuer(), row.publishedAt(), row.verifiedAt(),
                row.productGroup(), row.certificationType(), row.schemeName(), row.sourceUrl(),
                row.createdAt());
    }
}
