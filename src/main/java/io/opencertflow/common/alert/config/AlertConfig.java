package io.opencertflow.common.alert.config;

import io.opencertflow.common.alert.adapter.out.webhook.AlertProperties;
import io.opencertflow.common.alert.adapter.out.webhook.DiscordWebhookAlertAdapter;
import io.opencertflow.common.alert.application.port.out.OpsAlertPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * 운영자 알림(#39) 조립. <b>기본은 꺼짐이다</b> — webhook-url이 있을 때만 실제 어댑터가
 * 조립되고, 없으면 무음(NoOp) 구현이 대신 들어가 주입처는 항상 빈을 받되 아무 일도
 * 일어나지 않는다. 팀원 로컬·테스트·CI가 조용한 이유다.
 */
@Configuration
@EnableConfigurationProperties(AlertProperties.class)
public class AlertConfig {

    private static final Logger log = LoggerFactory.getLogger(AlertConfig.class);

    /**
     * 값의 유무로 구현을 고른다. 조건부 빈 대신 코드 검사인 이유: 컴포즈 전달 통로
     * {@code ${VAR:-}}는 값이 없으면 <b>빈 문자열</b>을 주입하는데,
     * {@code @ConditionalOnProperty}는 빈 문자열도 "존재"로 판정해 빈 주소로 활성돼 버린다.
     * {@code hasText}는 null과 빈 값을 모두 미설정으로 취급한다.
     *
     * <p>활성이든 비활성이든 부팅 로그에 한 줄 남는다 — 배포 직후 설정 누락을 눈으로 잡는 용도.
     */
    @Bean
    public OpsAlertPort opsAlertPort(WebClient.Builder builder, AlertProperties properties) {
        if (!StringUtils.hasText(properties.webhookUrl())) {
            log.info("운영자 알림 비활성 — OPENCERTFLOW_ALERT_WEBHOOK_URL 미설정.");
            return (title, message) -> {
            };
        }
        log.info("운영자 알림 활성 — 디스코드 웹훅으로 발송한다.");
        return new DiscordWebhookAlertAdapter(builder, properties.webhookUrl());
    }
}
