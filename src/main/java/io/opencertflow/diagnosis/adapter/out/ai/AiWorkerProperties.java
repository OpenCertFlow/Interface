package io.opencertflow.diagnosis.adapter.out.ai;

import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * AI/RAG 워커 연동 설정. {@code opencertflow.ai-worker.*}에 바인딩된다.
 *
 * @param baseUrl        워커 기본 URL (실제 어댑터가 사용)
 * @param searchTimeout  근거 검색 응답 예산. 초과 시 근거 없이 진행(ADR-0004)
 * @param narrateTimeout 문장화 응답 예산. 초과 시 템플릿 폴백
 */
@Validated
@ConfigurationProperties(prefix = "opencertflow.ai-worker")
public record AiWorkerProperties(

        @NotNull @DefaultValue("http://localhost:8000") String baseUrl,

        @NotNull @DefaultValue("2s") Duration searchTimeout,

        @NotNull @DefaultValue("5s") Duration narrateTimeout) {
}
