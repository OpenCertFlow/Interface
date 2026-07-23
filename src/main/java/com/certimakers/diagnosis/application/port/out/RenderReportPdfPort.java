package com.certimakers.diagnosis.application.port.out;

import com.certimakers.diagnosis.domain.model.Diagnosis;

/**
 * 진단 리포트를 PDF 바이트로 그린다.
 *
 * <p>순수 CPU 연산이며 IO가 없다. 큰 리포트는 CPU를 오래 쓰므로 호출자가 BlockingBridge로 감싼다.
 */
public interface RenderReportPdfPort {

    byte[] render(Diagnosis diagnosis);
}
