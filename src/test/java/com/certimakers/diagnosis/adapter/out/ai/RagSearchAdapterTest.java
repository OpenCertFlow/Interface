package com.certimakers.diagnosis.adapter.out.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.certimakers.common.domain.error.ExternalSystemException;
import com.certimakers.diagnosis.application.port.out.EvidenceQuery;
import com.certimakers.diagnosis.domain.model.CertificationType;
import com.certimakers.diagnosis.domain.model.Evidence;
import com.certimakers.diagnosis.domain.model.ProductGroup;
import com.certimakers.diagnosis.domain.model.SchemeCode;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * WebClient를 스텁 ExchangeFunction으로 구성해 워커 없이 어댑터를 검증한다. MockWebServer 없이
 * 응답 본문을 직접 흉내 낸다.
 */
class RagSearchAdapterTest {

    private final EvidenceQuery query = new EvidenceQuery(
            ProductGroup.SMALL_APPLIANCE,
            Set.of(SchemeCode.of("KC_SAFETY_CONFIRM_ELECTRIC")),
            Set.of(CertificationType.SAFETY_CONFIRM),
            List.of("DOCUMENTS"));

    private RagSearchAdapter adapterReturning(ClientResponse response) {
        ExchangeFunction exchange = request -> Mono.just(response);
        WebClient client = WebClient.builder().exchangeFunction(exchange).build();
        return new RagSearchAdapter(client);
    }

    @Test
    @DisplayName("정상 응답을 도메인 Evidence 목록으로 매핑한다")
    void 정상응답_매핑() {
        String body = """
                {"evidences":[
                    {"sourceDocumentId":"doc-1","sectionType":"DOCUMENTS",
                     "snippet":"안전확인 대상 전기용품...","sourceUrl":"https://safetykorea.kr/1","relevance":0.83}
                ]}""";
        RagSearchAdapter adapter = adapterReturning(jsonResponse(HttpStatus.OK, body));

        StepVerifier.create(adapter.search(query))
                .assertNext(evidences -> {
                    assertThat(evidences).hasSize(1);
                    Evidence evidence = evidences.get(0);
                    assertThat(evidence.sectionType()).isEqualTo("DOCUMENTS");
                    assertThat(evidence.sourceUrl().toString()).isEqualTo("https://safetykorea.kr/1");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("빈 근거 목록도 정상 처리한다 — 실패가 아니다")
    void 빈목록_정상() {
        RagSearchAdapter adapter = adapterReturning(jsonResponse(HttpStatus.OK, "{\"evidences\":[]}"));

        StepVerifier.create(adapter.search(query))
                .assertNext(evidences -> assertThat(evidences).isEmpty())
                .verifyComplete();
    }

    @Test
    @DisplayName("5xx 응답은 ExternalSystemException으로 바뀐다 — 서비스가 폴백을 결정하도록")
    void 오류응답_예외() {
        RagSearchAdapter adapter = adapterReturning(jsonResponse(HttpStatus.INTERNAL_SERVER_ERROR, "{}"));

        StepVerifier.create(adapter.search(query))
                .expectError(ExternalSystemException.class)
                .verify();
    }

    private ClientResponse jsonResponse(HttpStatus status, String body) {
        return ClientResponse.create(status)
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .body(body)
                .build();
    }
}
