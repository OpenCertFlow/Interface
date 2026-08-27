package io.opencertflow.common.alert.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import io.opencertflow.common.alert.adapter.out.webhook.AlertProperties;
import io.opencertflow.common.alert.adapter.out.webhook.DiscordWebhookAlertAdapter;
import io.opencertflow.common.alert.application.port.out.OpsAlertPort;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * 운영자 알림 조립(#39)을 검증한다 — 값의 유무가 구현을 고르고, 전송 실패는 밖으로 새지 않는다.
 */
class AlertConfigTest {

    private final AlertConfig config = new AlertConfig();

    private OpsAlertPort portFor(String webhookUrl) {
        return config.opsAlertPort(
                WebClient.builder(), new AlertProperties(webhookUrl, Duration.ofMinutes(1)));
    }

    @Test
    @DisplayName("url이 없으면 무음 구현이 조립된다")
    void 미설정이면_무음() {
        assertThat(portFor(null)).isNotInstanceOf(DiscordWebhookAlertAdapter.class);
    }

    @Test
    @DisplayName("빈 문자열도 미설정으로 취급한다 — @ConditionalOnProperty였다면 빈 주소로 활성됐다")
    void 빈_문자열도_무음() {
        assertThat(portFor("")).isNotInstanceOf(DiscordWebhookAlertAdapter.class);
        assertThat(portFor("   ")).isNotInstanceOf(DiscordWebhookAlertAdapter.class);
    }

    @Test
    @DisplayName("실제 값이 있으면 디스코드 어댑터가 조립된다")
    void 값이_있으면_디스코드() {
        assertThat(portFor("https://discord.test/webhook"))
                .isInstanceOf(DiscordWebhookAlertAdapter.class);
    }

    @Test
    @DisplayName("무음 구현은 불러도 아무 일도 없다 — 주입처가 null 검사 없이 항상 부를 수 있다")
    void 무음은_안전() {
        assertThatCode(() -> portFor(null).send("제목", "내용")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("전송 실패는 예외로 새지 않는다 — 알림 때문에 본체가 죽으면 본말전도")
    void 전송_실패는_삼킨다() {
        // 아무도 안 듣는 주소 — 발사 후 잊기라 호출 자체가 즉시 돌아오고, 비동기 실패는 삼켜진다.
        OpsAlertPort deadWebhook = portFor("http://localhost:1");

        assertThatCode(() -> deadWebhook.send("회로 상태 전환", "테스트")).doesNotThrowAnyException();
    }
}
