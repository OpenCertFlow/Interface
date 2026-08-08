package io.opencertflow.diagnosis.config;

import io.opencertflow.diagnosis.adapter.out.ai.AiWorkerProperties;
import io.opencertflow.diagnosis.adapter.out.persistence.diagnosis.DiagnosisMapper;
import io.opencertflow.diagnosis.adapter.out.persistence.rule.RuleJsonCodec;
import io.opencertflow.diagnosis.application.DiagnosisPolicy;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * 진단 컨텍스트의 구성 루트. 어댑터 설정(AiWorkerProperties)을 애플리케이션 정책(DiagnosisPolicy)으로
 * 옮겨, 서비스가 어댑터에 의존하지 않고도 타임아웃 값을 받게 한다.
 */
@Configuration
@EnableConfigurationProperties(AiWorkerProperties.class)
public class DiagnosisConfig {

    @Bean
    public DiagnosisPolicy diagnosisPolicy(AiWorkerProperties properties) {
        return new DiagnosisPolicy(properties.searchTimeout(), properties.narrateTimeout());
    }

    /** 룰 JSON 코덱. 스프링이 관리하는 ObjectMapper를 재사용한다. */
    @Bean
    public RuleJsonCodec ruleJsonCodec(ObjectMapper objectMapper) {
        return new RuleJsonCodec(objectMapper);
    }

    /** 진단 애그리거트 ↔ JPA 엔티티 매퍼. 상태가 없어 싱글턴 빈으로 둔다. */
    @Bean
    public DiagnosisMapper diagnosisMapper() {
        return new DiagnosisMapper();
    }

    /** AI 워커 호출용 WebClient. 공통 빌더(traceId 전파·타임아웃 포함)에 base URL만 얹는다. */
    @Bean
    public WebClient aiWorkerWebClient(WebClient.Builder webClientBuilder, AiWorkerProperties properties) {
        return webClientBuilder.baseUrl(properties.baseUrl()).build();
    }
}
