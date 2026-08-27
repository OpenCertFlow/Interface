package io.opencertflow.common.alert.adapter.in.scheduler;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.opencertflow.common.alert.application.port.out.OpsAlertPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * ERROR 로그 발생을 폴링으로 감지해 운영자에게 통보한다(#39).
 *
 * <p><b>로그 경로에 침입하지 않는다.</b> 어펜더 대신, 스프링이 이미 세고 있는
 * {@code logback.events{level=error}} 카운터를 주기적으로 읽어 증가분을 한 건으로 요약한다.
 * 폴링 창(기본 1분)이 곧 요약 창이라, 같은 장애로 ERROR가 연발해도 알림은 창당 하나다.
 *
 * <p><b>WARN은 세지 않는다.</b> 폴백 경고(RAG·LLM 실패 시 warn)는 설계된 정상 경로라
 * 알림으로 보내면 소음이 된다. 예상 밖의 사건은 ERROR로 찍힌다는 전제다.
 *
 * <p>webhook-url이 없으면 이 빈 자체가 생성되지 않는다 — 폴링도 발송도 통째로 없다.
 * 팀원 로컬·테스트·CI가 조용한 이유다.
 */
@Component
// @ConditionalOnProperty는 빈 문자열도 "존재"로 판정한다 — 컴포즈 전달 통로가 값 없을 때
// 빈 문자열을 주입하므로, hasText로 "실제 값이 있을 때"만 이 빈이 생성되게 한다.
@ConditionalOnExpression(
        "T(org.springframework.util.StringUtils).hasText('${opencertflow.alert.webhook-url:}')")
public class ErrorLogAlertScheduler {

    private final MeterRegistry meterRegistry;
    private final OpsAlertPort alertPort;

    /** 직전 폴링 시점의 누적 ERROR 수. @Scheduled는 단일 스레드라 동기화 불필요. */
    private double lastCount;

    public ErrorLogAlertScheduler(MeterRegistry meterRegistry, OpsAlertPort alertPort) {
        this.meterRegistry = meterRegistry;
        this.alertPort = alertPort;
    }

    @Scheduled(fixedDelayString = "${opencertflow.alert.poll-interval:1m}")
    void poll() {
        double now = currentErrorCount();
        if (now > lastCount) {
            alertPort.send("ERROR 발생",
                    "지난 폴링 이후 %d건 — 로그 확인 필요".formatted((long) (now - lastCount)));
        }
        lastCount = now;
    }

    private double currentErrorCount() {
        return meterRegistry.find("logback.events").tag("level", "error").counters()
                .stream().mapToDouble(Counter::count).sum();
    }
}
