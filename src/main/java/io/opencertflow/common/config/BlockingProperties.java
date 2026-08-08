package io.opencertflow.common.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * 블로킹 격리 스케줄러 설정.
 *
 * @param jdbcPoolSize  {@code jdbcScheduler}의 스레드 수. <b>HikariCP의
 *                      {@code maximum-pool-size}와 같은 값이어야 한다.</b> 더 크면 초과 스레드가
 *                      커넥션을 기다리며 메모리만 먹고, 더 작으면 커넥션이 논다.
 *                      {@link BlockingBridgeConfig}가 시작 시 불일치를 경고한다.
 * @param queueCapacity 스레드가 모두 바쁠 때 대기시킬 작업 수. 초과 시 즉시 거부되어
 *                      요청이 무한정 쌓이지 않는다.
 * @param ttlSeconds    유휴 스레드 회수 시간.
 */
@Validated
@ConfigurationProperties(prefix = "opencertflow.blocking")
public record BlockingProperties(

        @Min(1) @Max(100) @DefaultValue("10") int jdbcPoolSize,

        @Min(0) @DefaultValue("1000") int queueCapacity,

        @Min(1) @DefaultValue("60") int ttlSeconds) {
}
