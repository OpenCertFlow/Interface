package io.opencertflow.consulting.application.port.out;

import io.opencertflow.diagnosis.domain.model.Diagnosis;

/** 상담 브리핑 PDF 렌더링. 그리는 방법은 어댑터가 안다. */
public interface RenderBriefingPdfPort {

    /**
     * @param leadStatus 상담 상태. 어느 단계에서 뽑은 브리핑인지 문서에 남긴다
     */
    byte[] render(Diagnosis diagnosis, String leadStatus);
}
