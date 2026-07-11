package com.certimakers.common.config;

import com.certimakers.common.adapter.in.web.trace.TraceId;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

/**
 * 외부 호출용 {@link WebClient} 기본 설정.
 *
 * <p>여기서 정하는 타임아웃은 <b>연결·소켓 수준</b>의 상한이다. 개별 호출의 응답 시간 예산
 * (RAG 2초, 문장화 5초)은 각 어댑터가 {@code .timeout(...)}으로 따로 건다. 두 층위를 섞지 않는다.
 */
@Configuration
public class WebClientConfig {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(2);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(10);

    @Bean
    public WebClient.Builder webClientBuilder() {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) CONNECT_TIMEOUT.toMillis())
                .doOnConnected(connection ->
                        connection.addHandlerLast(
                                new ReadTimeoutHandler(READ_TIMEOUT.toSeconds(), TimeUnit.SECONDS)));

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .filter(traceIdPropagation());
    }

    /**
     * 백엔드가 만든 traceId를 AI 워커로 전파한다. 워커가 이 값을 로그에 찍으면 두 서비스의 로그가
     * 하나의 요청으로 이어진다. 해커톤 규모에서 필요한 분산 추적은 이것이 전부다(ADR-0004).
     */
    private ExchangeFilterFunction traceIdPropagation() {
        return (request, next) -> Mono.deferContextual(context -> {
            String traceId = TraceId.from(context);
            return next.exchange(
                    ClientRequest.from(request).header(TraceId.HEADER, traceId).build());
        });
    }
}
