package io.opencertflow.common.adapter.in.web.trace;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

/**
 * 모든 요청에 traceId를 부여하고 Reactor Context와 응답 헤더에 실는다.
 *
 * <p>클라이언트가 {@code X-Trace-Id}를 보내면 그것을 이어받는다. Android 앱이 자체 요청 ID를
 * 가진 경우 앱 로그와 서버 로그가 같은 키로 이어진다.
 *
 * <p>백엔드 → AI 워커 호출에도 같은 헤더가 전파된다({@code WebClientConfig}). 이것이 분산 추적의
 * 최소 형태이며, 해커톤 규모에서는 이것으로 충분하다(ADR-0004).
 *
 * <p>이 필터는 <b>네티 이벤트 루프에서 모든 요청마다</b> 실행된다. 따라서 추적 ID 생성에
 * {@code SecureRandom} 기반 {@code IdGenerator}를 쓰지 않고 {@link TraceIdFactory}를 쓴다 —
 * 이유는 그 클래스에 적어 두었다.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdWebFilter implements WebFilter {

    private final TraceIdFactory traceIdFactory;

    public TraceIdWebFilter(TraceIdFactory traceIdFactory) {
        this.traceIdFactory = traceIdFactory;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String inbound = exchange.getRequest().getHeaders().getFirst(TraceId.HEADER);
        String traceId = StringUtils.hasText(inbound) ? inbound : traceIdFactory.newTraceId();

        exchange.getResponse().getHeaders().set(TraceId.HEADER, traceId);
        return chain.filter(exchange).contextWrite(Context.of(TraceId.CONTEXT_KEY, traceId));
    }
}
