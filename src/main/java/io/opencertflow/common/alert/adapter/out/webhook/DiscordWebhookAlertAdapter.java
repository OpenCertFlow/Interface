package io.opencertflow.common.alert.adapter.out.webhook;

import io.opencertflow.common.alert.application.port.out.OpsAlertPort;
import java.time.Duration;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * {@link OpsAlertPort}의 디스코드 웹훅 구현. {@code {"content": ...}} 하나를 POST한다.
 *
 * <p><b>발사 후 잊는다(fire-and-forget).</b> {@code subscribe()}로 비동기 전송하고 호출자를
 * 기다리게 하지 않는다 — 회로 전환 콜백이나 이벤트 루프에서 불려도 본체를 막지 않는다.
 * 실패는 로그만 남기고 삼킨다. 웹훅이 죽어도 진단은 돌아야 한다.
 *
 * <p>빈 등록은 {@code AlertConfig}가 webhook-url 존재 시에만 한다 — 이 클래스에는
 * 스프링 애노테이션이 없다.
 */
public class DiscordWebhookAlertAdapter implements OpsAlertPort {

    private static final Logger log = LoggerFactory.getLogger(DiscordWebhookAlertAdapter.class);
    private static final Duration SEND_TIMEOUT = Duration.ofSeconds(5);

    private final WebClient webClient;

    public DiscordWebhookAlertAdapter(WebClient.Builder builder, String webhookUrl) {
        this.webClient = builder.baseUrl(webhookUrl).build();
    }

    @Override
    public void send(String title, String message) {
        webClient.post()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("content", "**" + title + "**\n" + message))
                .retrieve()
                .toBodilessEntity()
                .timeout(SEND_TIMEOUT)
                .doOnError(error -> log.warn("운영자 알림 전송 실패 — 삼킨다. title={}, cause={}",
                        title, error.toString()))
                .onErrorResume(error -> Mono.empty())
                .subscribe();
    }
}
