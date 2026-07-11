package com.certimakers.diagnosis.adapter.out.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.certimakers.common.domain.error.ExternalSystemException;
import com.certimakers.diagnosis.application.port.out.NarrationRequest;
import com.certimakers.diagnosis.domain.model.ProductProfile;
import com.certimakers.diagnosis.domain.model.ReadinessScore;
import com.certimakers.diagnosis.domain.ProductProfileFixtures;
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

class LlmNarrationAdapterTest {

    private NarrationRequest request() {
        ProductProfile profile = ProductProfileFixtures.hairDryer(Set.of());
        return new NarrationRequest(
                profile, ReadinessScore.of(3, 7), List.of(), List.of(), List.of(), List.of());
    }

    private LlmNarrationAdapter adapterReturning(ClientResponse response) {
        ExchangeFunction exchange = req -> Mono.just(response);
        WebClient client = WebClient.builder().exchangeFunction(exchange).build();
        return new LlmNarrationAdapter(client);
    }

    @Test
    @DisplayName("정상 응답을 Narration으로 매핑한다 — LLM 생성이므로 templateFallback=false")
    void 정상응답_매핑() {
        String body = """
                {"summary":"220V 드라이기는 안전확인 대상으로 보입니다.",
                 "nextActions":["시험성적서를 신청하세요."],
                 "preConsultQuestions":["정격전압이 정확히 몇 V인가요?"],
                 "disclaimer":"사전 점검 지표입니다.","modelId":"claude-opus-4-8"}""";
        LlmNarrationAdapter adapter = adapterReturning(jsonResponse(HttpStatus.OK, body));

        StepVerifier.create(adapter.narrate(request()))
                .assertNext(narration -> {
                    assertThat(narration.summary()).contains("안전확인");
                    assertThat(narration.isTemplateFallback()).isFalse();
                    assertThat(narration.modelId()).isEqualTo("claude-opus-4-8");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("504 응답은 ExternalSystemException으로 바뀐다")
    void 오류응답_예외() {
        LlmNarrationAdapter adapter = adapterReturning(jsonResponse(HttpStatus.GATEWAY_TIMEOUT, "{}"));

        StepVerifier.create(adapter.narrate(request()))
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
