package io.opencertflow.diagnosis.application.port.in;

import reactor.core.publisher.Mono;

/**
 * 진단 리포트를 PDF로 내려받는다.
 *
 * <p>결과를 저장하지 않는다. 리포트는 저장된 진단에서 매번 다시 그리므로, 룰셋이나 표현이 바뀌어도
 * 최신 형식으로 나온다. PDF를 파일로 보관하면 그 시점 형식에 묶인다.
 */
public interface ExportReportPdfQuery {

    Mono<ReportPdf> export(String diagnosisId, String viewerUserId);

    /**
     * @param fileName 다운로드 시 제안할 파일명
     * @param content  PDF 바이트
     */
    record ReportPdf(String fileName, byte[] content) {
    }
}
