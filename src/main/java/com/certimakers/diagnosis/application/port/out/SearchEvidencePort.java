package com.certimakers.diagnosis.application.port.out;

import com.certimakers.diagnosis.domain.model.Evidence;
import java.util.List;
import reactor.core.publisher.Mono;

/**
 * 아웃바운드 포트: RAG 워커에서 공식 문서 근거를 검색한다. 논블로킹(WebClient).
 *
 * <p>실패·타임아웃은 폴백 대상이다. 어댑터는 {@code ExternalSystemException}으로 실패를 알리고,
 * 서비스가 근거 없이 진행하기로 결정한다(degraded.evidence). 어댑터가 스스로 빈 결과를 지어내지
 * 않는다 — 폴백은 애플리케이션의 정책 결정이다(ADR-0004).
 *
 * <p>빈 목록과 실패는 다르다. 임계 유사도를 넘는 근거가 없어 빈 목록이 오는 것은 정상 응답이며
 * degraded가 아니다. 근거 부재는 이후 전문가 확인 항목으로 이어진다.
 */
public interface SearchEvidencePort {

    Mono<List<Evidence>> search(EvidenceQuery query);
}
