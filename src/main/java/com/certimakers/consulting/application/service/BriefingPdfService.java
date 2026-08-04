package com.certimakers.consulting.application.service;

import com.certimakers.common.application.annotation.UseCase;
import com.certimakers.common.application.support.BlockingBridge;
import com.certimakers.common.domain.error.BusinessException;
import com.certimakers.consulting.application.port.in.ExportBriefingPdfQuery;
import com.certimakers.consulting.application.port.in.ManageConsultingUseCase;
import com.certimakers.consulting.application.port.out.RenderBriefingPdfPort;
import com.certimakers.diagnosis.application.port.out.LoadDiagnosisPort;
import com.certimakers.diagnosis.domain.model.DiagnosisId;
import reactor.core.publisher.Mono;

/**
 * 상담 준비 브리핑 PDF를 만든다(F-WCON-012).
 *
 * <p>상담 리드에서 진단을 찾아 브리핑으로 옮긴다. 리드에 진단이 연결되어 있지 않으면 브리핑을
 * 만들 수 없다 — 억지로 빈 문서를 내지 않고 명확히 거절한다.
 */
@UseCase
public class BriefingPdfService implements ExportBriefingPdfQuery {

    private final ManageConsultingUseCase consultingUseCase;
    private final LoadDiagnosisPort loadDiagnosisPort;
    private final RenderBriefingPdfPort renderPort;
    private final BlockingBridge blockingBridge;

    public BriefingPdfService(
            ManageConsultingUseCase consultingUseCase,
            LoadDiagnosisPort loadDiagnosisPort,
            RenderBriefingPdfPort renderPort,
            BlockingBridge blockingBridge) {
        this.consultingUseCase = consultingUseCase;
        this.loadDiagnosisPort = loadDiagnosisPort;
        this.renderPort = renderPort;
        this.blockingBridge = blockingBridge;
    }

    @Override
    public Mono<byte[]> render(String leadId) {
        return consultingUseCase.get(leadId).flatMap(lead -> {
            if (lead.diagnosisId() == null || lead.diagnosisId().isBlank()) {
                return Mono.error(BusinessException.invalid(
                        "이 상담에는 연결된 진단이 없어 브리핑을 만들 수 없습니다."));
            }
            return blockingBridge.mono(() ->
                            loadDiagnosisPort.load(DiagnosisId.of(Long.parseLong(lead.diagnosisId()))))
                    .switchIfEmpty(Mono.error(BusinessException.invalid(
                            "연결된 진단을 찾을 수 없습니다: " + lead.diagnosisId())))
                    .map(diagnosis -> renderPort.render(diagnosis, lead.status()));
        });
    }
}
