package io.opencertflow.audit.adapter.in.web;

import io.opencertflow.audit.application.port.in.RecordAuditUseCase;
import io.opencertflow.audit.application.port.in.RecordAuditUseCase.AuditCommand;
import java.util.Set;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * 관리자 변경 행위를 자동 기록하는 필터(F-BE-018). /api/v1/admin/** 의 변경 요청만 남긴다.
 *
 * <p>시큐리티 필터가 먼저 실행돼 리액터 컨텍스트에 인증을 넣으므로, 여기서 행위자를 읽을 수 있다.
 * 기록은 응답이 끝난 뒤 이뤄지며, 실패해도 원 요청을 깨뜨리지 않는다.
 */
@Component
public class AuditLogWebFilter implements WebFilter {

    private static final String ADMIN_PREFIX = "/api/v1/admin/";
    private static final Set<HttpMethod> MUTATING =
            Set.of(HttpMethod.POST, HttpMethod.PUT, HttpMethod.PATCH, HttpMethod.DELETE);

    private final RecordAuditUseCase recordAuditUseCase;

    public AuditLogWebFilter(RecordAuditUseCase recordAuditUseCase) {
        this.recordAuditUseCase = recordAuditUseCase;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        HttpMethod method = exchange.getRequest().getMethod();
        if (!path.startsWith(ADMIN_PREFIX) || !MUTATING.contains(method)) {
            return chain.filter(exchange);
        }
        return chain.filter(exchange)
                .then(Mono.defer(() -> recordAudit(exchange, method.name(), path)));
    }

    private Mono<Void> recordAudit(ServerWebExchange exchange, String method, String path) {
        HttpStatusCode status = exchange.getResponse().getStatusCode();
        Integer statusValue = status != null ? status.value() : null;
        return currentActor()
                .flatMap(actor -> recordAuditUseCase.record(
                        new AuditCommand(actor, method, path, statusValue)));
    }

    private Mono<String> currentActor() {
        return ReactiveSecurityContextHolder.getContext()
                .map(context -> context.getAuthentication())
                .filter(authentication -> authentication != null)
                .map(Authentication::getName)
                .defaultIfEmpty("anonymous");
    }
}
