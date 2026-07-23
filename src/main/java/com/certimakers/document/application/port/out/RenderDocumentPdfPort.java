package com.certimakers.document.application.port.out;

import com.certimakers.document.domain.model.FormValues;
import java.time.Instant;

/**
 * 채워진 양식을 PDF 바이트로 그린다.
 *
 * <p>순수 CPU 연산이며 IO가 없다. 다만 큰 문서는 CPU를 오래 쓰므로 호출자가 BlockingBridge로 감싼다.
 */
public interface RenderDocumentPdfPort {

    byte[] render(FormValues values, String issuerNickname, Instant issuedAt);
}
