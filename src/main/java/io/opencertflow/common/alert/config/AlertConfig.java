package io.opencertflow.common.alert.config;

import io.opencertflow.common.alert.adapter.out.webhook.AlertProperties;
import io.opencertflow.common.alert.adapter.out.webhook.DiscordWebhookAlertAdapter;
import io.opencertflow.common.alert.application.port.out.OpsAlertPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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

    @Bean
    @ConditionalOnProperty(prefix = "opencertflow.alert", name = "webhook-url")
    public OpsAlertPort discordAlert(WebClient.Builder builder, AlertProperties properties) {
        log.info("운영자 알림 활성 — 디스코드 웹훅으로 발송한다.");
        return new DiscordWebhookAlertAdapter(builder, properties.webhookUrl());
    }

    /** webhook-url 미설정 시의 무음 구현. 주입처가 null 검사 없이 항상 부를 수 있게 한다. */
    @Bean
    @ConditionalOnMissingBean(OpsAlertPort.class)
    public OpsAlertPort noOpAlert() {
        return (title, message) -> {
        };
    }
}
