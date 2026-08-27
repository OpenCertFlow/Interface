package io.opencertflow.common.alert.adapter.out.webhook;

import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * 운영자 알림 설정. {@code opencertflow.alert.*}에 바인딩된다.
 *
 * @param webhookUrl   디스코드 웹훅 URL. <b>시크릿이라 yml에 커밋하지 않고 환경변수
 *                     {@code OPENCERTFLOW_ALERT_WEBHOOK_URL}로만 주입한다.</b> 값의 존재가
 *                     알림 기능 전체의 켜짐 스위치다 — 없으면 발송·폴링이 통째로 비활성이라
 *                     팀원 로컬·테스트·CI는 무음이다. 따라서 nullable이다
 * @param pollInterval ERROR 카운터 확인 주기. 하한 근거: 수신자의 반응 시간(분 단위)보다
 *                     짧게 조여봐야 전체 대응은 안 빨라지고, 폴링 창이 곧 요약 창이라
 *                     짧을수록 같은 장애가 여러 알림으로 쪼개진다. 상한 근거: 감지 지연이
 *                     대응 시간과 맞먹으면 손해. 그 사이의 보수적 기본값이 1분이다
 */
@Validated
@ConfigurationProperties(prefix = "opencertflow.alert")
public record AlertProperties(

        String webhookUrl,

        @NotNull @DefaultValue("1m") Duration pollInterval) {
}
