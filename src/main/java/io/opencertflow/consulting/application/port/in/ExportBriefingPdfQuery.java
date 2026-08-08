package io.opencertflow.consulting.application.port.in;

import reactor.core.publisher.Mono;

/**
 * 상담 준비 브리핑을 PDF로 뽑는다(F-WCON-012).
 *
 * <p>기획서 2.5가 말하는 "상담 준비 브리핑" — 사용자가 동의한 제품 입력, Rule 결과, 누락자료,
 * 선정된 공식 근거, 전문가 질문 — 을 컨설턴트가 상담 자리에 들고 갈 수 있는 형태로 만든다.
 * 진단 리포트 PDF는 소공인용이고 이것은 <b>컨설턴트용</b>이다. 담는 것이 다르다.
 *
 * <p>연락처는 넣지 않는다. 인쇄물은 서비스 밖으로 나가 통제할 수 없고, 상담 자리에서 필요한 것은
 * 제품과 쟁점이지 개인정보가 아니다.
 */
public interface ExportBriefingPdfQuery {

    Mono<byte[]> render(String leadId);
}
