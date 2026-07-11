package com.certimakers.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * BlockHound가 실제로 설치되어 있는지 확인한다.
 *
 * <p>ADR-0002는 BlockHound를 "이벤트 루프 블로킹의 1차 방어선"으로 삼는다. 의존성만 있고 에이전트가
 * 붙지 않으면(예: {@code -XX:+AllowRedefinitionToAddDeleteMethods} 누락) 아무 경고 없이 조용히
 * 무력화된다. 그때부터 우리는 지켜지지 않는 규칙을 지켜진다고 믿게 된다. 그래서 방어선 자체를 테스트한다.
 */
class BlockHoundInstalledTest {

    @Test
    @DisplayName("논블로킹 스레드에서 블로킹 호출을 하면 BlockHound가 잡아낸다")
    void 블로킹_호출을_감지한다() {
        Mono<String> blockingOnParallelScheduler = Mono.fromCallable(() -> {
                    Thread.sleep(1); // 이벤트 루프에서 절대 일어나면 안 되는 일
                    return "이 값은 반환되지 않아야 한다";
                })
                .subscribeOn(Schedulers.parallel());

        assertThatThrownBy(() -> blockingOnParallelScheduler.block(Duration.ofSeconds(5)))
                .as("BlockHound가 설치되지 않았다면 이 호출은 조용히 성공한다")
                .hasMessageContaining("Blocking call");
    }

    @Test
    @DisplayName("boundedElastic 스케줄러에서의 블로킹은 허용된다 — jdbcScheduler가 여기 속한다")
    void boundedElastic에서는_블로킹이_허용된다() {
        String result = Mono.fromCallable(() -> {
                    Thread.sleep(1);
                    return "ok";
                })
                .subscribeOn(Schedulers.boundedElastic())
                .block(Duration.ofSeconds(5));

        assertThat(result).isEqualTo("ok");
    }
}
