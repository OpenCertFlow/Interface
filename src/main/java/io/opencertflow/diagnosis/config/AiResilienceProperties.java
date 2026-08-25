package io.opencertflow.diagnosis.config;

import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * AI 워커 회로 차단기 설정. {@code opencertflow.ai-worker.resilience.*}에 바인딩된다.
 *
 * <p>초기값(창 10건·실패율 50%·차단 30초)은 오탐(일시 요동으로 멀쩡한 워커를 차단)과
 * 늑장(죽은 워커에 계속 대기) 사이의 보수적 절충이며, 지표를 보고 조정한다.
 *
 * @param slidingWindowSize       실패율 판단에 쓸 최근 호출 수
 * @param minimumNumberOfCalls    실패율 계산을 시작할 최소 호출 수. <b>라이브러리 기본값이 100이라</b>
 *                                명시하지 않으면 소규모 트래픽에서 회로가 영영 열리지 않는다
 * @param failureRateThreshold    이 비율(%) 이상 실패하면 회로가 열린다
 * @param waitDurationInOpen      열린 뒤 시험 호출까지 기다리는 시간
 * @param permittedCallsInHalfOpen 반열림 상태에서 통과시킬 시험 호출 수
 */
@Validated
@ConfigurationProperties(prefix = "opencertflow.ai-worker.resilience")
public record AiResilienceProperties(

        @NotNull @DefaultValue("10") Integer slidingWindowSize,

        @NotNull @DefaultValue("5") Integer minimumNumberOfCalls,

        @NotNull @DefaultValue("50") Float failureRateThreshold,

        @NotNull @DefaultValue("30s") Duration waitDurationInOpen,

        @NotNull @DefaultValue("3") Integer permittedCallsInHalfOpen) {
}
