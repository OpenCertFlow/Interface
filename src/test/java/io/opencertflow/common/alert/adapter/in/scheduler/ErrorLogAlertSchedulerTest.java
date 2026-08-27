package io.opencertflow.common.alert.adapter.in.scheduler;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.opencertflow.common.alert.application.port.out.OpsAlertPort;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * ERROR 폴링 알림(#39)의 요약 계산과 조건부 조립을 검증한다.
 *
 * <p>폴링 로직은 스프링 없이 직접 부르고, "webhook-url이 없으면 빈 자체가 없다"는
 * {@link ApplicationContextRunner}로 컨텍스트를 조립해 본다 — 조건 애노테이션은 빈 생성
 * 전에 평가되므로 인스턴스 테스트로는 검증할 수 없다.
 */
class ErrorLogAlertSchedulerTest {

    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    private final List<String> sent = new ArrayList<>();
    private final OpsAlertPort capturingPort = (title, message) -> sent.add(message);

    private Counter errorCounter() {
        return meterRegistry.counter("logback.events", "level", "error");
    }

    // ── 폴링 요약 계산 ────────────────────────────────────────────

    @Test
    @DisplayName("ERROR가 늘지 않았으면 발송하지 않는다")
    void 증가가_없으면_무발송() {
        ErrorLogAlertScheduler scheduler = new ErrorLogAlertScheduler(meterRegistry, capturingPort);

        scheduler.poll();

        assertThat(sent).isEmpty();
    }

    @Test
    @DisplayName("폴링 창 동안의 증가분을 한 건으로 요약한다")
    void 증가분을_한_건으로_요약() {
        ErrorLogAlertScheduler scheduler = new ErrorLogAlertScheduler(meterRegistry, capturingPort);
        scheduler.poll();   // 기준점

        errorCounter().increment(3);
        scheduler.poll();

        assertThat(sent).hasSize(1);
        assertThat(sent.get(0)).contains("3건");
    }

    @Test
    @DisplayName("다음 창은 누적이 아니라 그 창의 증가분만 센다")
    void 창마다_증가분만() {
        ErrorLogAlertScheduler scheduler = new ErrorLogAlertScheduler(meterRegistry, capturingPort);
        errorCounter().increment(3);
        scheduler.poll();

        scheduler.poll();               // 증가 없음 → 무발송
        errorCounter().increment(2);
        scheduler.poll();               // 이번 창은 2건

        assertThat(sent).hasSize(2);
        assertThat(sent.get(1)).contains("2건");   // 5건(누적)이 아니다
    }

    // ── 조건부 조립: webhook-url이 곧 스위치 ─────────────────────

    @Configuration(proxyBeanMethods = false)
    @Import(ErrorLogAlertScheduler.class)
    static class SchedulerOnly {
    }

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withBean(MeterRegistry.class, SimpleMeterRegistry::new)
            .withBean(OpsAlertPort.class, () -> (title, message) -> {
            })
            .withUserConfiguration(SchedulerOnly.class);

    @Test
    @DisplayName("webhook-url이 없으면 스케줄러 빈 자체가 생성되지 않는다")
    void 미설정이면_빈_부재() {
        runner.run(context ->
                assertThat(context).doesNotHaveBean(ErrorLogAlertScheduler.class));
    }

    @Test
    @DisplayName("빈 문자열도 미설정으로 취급한다 — 컴포즈 전달 통로가 값 없을 때 빈 값을 주입한다")
    void 빈_문자열도_미설정() {
        runner.withPropertyValues("opencertflow.alert.webhook-url=")
                .run(context ->
                        assertThat(context).doesNotHaveBean(ErrorLogAlertScheduler.class));
    }

    @Test
    @DisplayName("실제 값이 있으면 스케줄러가 조립된다")
    void 값이_있으면_생성() {
        runner.withPropertyValues("opencertflow.alert.webhook-url=https://discord.test/webhook")
                .run(context ->
                        assertThat(context).hasSingleBean(ErrorLogAlertScheduler.class));
    }
}
