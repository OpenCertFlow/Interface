package com.certimakers.diagnosis.adapter.out.ai;

import com.certimakers.common.adapter.out.external.annotation.ExternalAdapter;
import com.certimakers.diagnosis.application.port.out.EvidenceQuery;
import com.certimakers.diagnosis.application.port.out.SearchEvidencePort;
import com.certimakers.diagnosis.domain.model.Evidence;
import java.net.URI;
import java.util.List;
import org.springframework.context.annotation.Profile;
import reactor.core.publisher.Mono;

/**
 * AI 워커가 없을 때 쓰는 근거 검색 스텁. {@code local} 프로파일에서만 활성화된다.
 *
 * <p>이 스텁이 있기에 백엔드는 RAG 워커 완성을 기다리지 않고 진단 흐름 전체를 돌리고 데모할 수
 * 있다(ADR-0004). 실제 {@code RagSearchAdapter}는 기본 프로파일에서 이 자리를 대신한다.
 */
@ExternalAdapter
@Profile("local")
public class StubEvidenceSearchAdapter implements SearchEvidencePort {

    @Override
    public Mono<List<Evidence>> search(EvidenceQuery query) {
        Evidence sample = new Evidence(
                "stub-doc-electric-safety",
                "DOCUMENTS",
                "안전확인대상 전기용품은 지정된 시험기관의 시험을 거쳐 안전확인신고를 하여야 한다. (스텁 데이터)",
                URI.create("https://www.safetykorea.kr/"),
                0.80);
        return Mono.just(List.of(sample));
    }
}
