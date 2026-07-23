package com.certimakers.diagnosis.application.service;

import com.certimakers.common.application.annotation.UseCase;
import com.certimakers.common.application.support.BlockingBridge;
import com.certimakers.common.domain.error.BusinessException;
import com.certimakers.diagnosis.application.port.in.ExportReportPdfQuery;
import com.certimakers.diagnosis.application.port.out.LoadDiagnosisPort;
import com.certimakers.diagnosis.application.port.out.RenderReportPdfPort;
import com.certimakers.diagnosis.domain.error.DiagnosisErrorCode;
import com.certimakers.diagnosis.domain.model.Diagnosis;
import com.certimakers.diagnosis.domain.model.DiagnosisId;
import java.util.UUID;
import reactor.core.publisher.Mono;

/**
 * 진단 리포트 PDF 내보내기.
 *
 * <p>조회와 렌더링 모두 이벤트 루프 밖에서 돌린다 — 조회는 JPA(블로킹)이고, 렌더링은 IO가 없지만
 * CPU를 오래 쓴다.
 */
@UseCase
public class ExportReportPdfService implements ExportReportPdfQuery {

    private final LoadDiagnosisPort loadDiagnosisPort;
    private final RenderReportPdfPort renderReportPdfPort;
    private final BlockingBridge blockingBridge;

    public ExportReportPdfService(
            LoadDiagnosisPort loadDiagnosisPort,
            RenderReportPdfPort renderReportPdfPort,
            BlockingBridge blockingBridge) {
        this.loadDiagnosisPort = loadDiagnosisPort;
        this.renderReportPdfPort = renderReportPdfPort;
        this.blockingBridge = blockingBridge;
    }

    @Override
    public Mono<ReportPdf> export(String diagnosisId) {
        DiagnosisId id = parseId(diagnosisId);

        // 포트는 없으면 null을 돌려주는 계약이다(LoadDiagnosisPort). 빈 Mono가 되므로 switchIfEmpty로 404를 낸다.
        return blockingBridge.mono(() -> loadDiagnosisPort.load(id))
                .switchIfEmpty(Mono.error(
                        new BusinessException(DiagnosisErrorCode.DIAGNOSIS_NOT_FOUND)))
                .map(diagnosis -> new ReportPdf(
                        fileNameOf(diagnosis), renderReportPdfPort.render(diagnosis)));
    }

    /** 제품명과 날짜를 파일명에 넣어 여러 리포트를 내려받아도 구분되게 한다. */
    private String fileNameOf(Diagnosis diagnosis) {
        String date = diagnosis.createdAt().toString().substring(0, 10);
        String product = diagnosis.profile().productName().replace(" ", "");
        return "진단리포트_%s_%s.pdf".formatted(product, date);
    }

    private DiagnosisId parseId(String rawId) {
        try {
            return DiagnosisId.of(UUID.fromString(rawId));
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BusinessException(DiagnosisErrorCode.DIAGNOSIS_NOT_FOUND);
        }
    }
}
