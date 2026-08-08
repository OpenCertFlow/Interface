package io.opencertflow.auth.config;

import io.opencertflow.auth.application.port.out.TokenProviderPort;
import io.opencertflow.auth.application.port.out.TokenProviderPort.AuthenticatedUser;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * {@code Authorization: Bearer <token>} 헤더를 읽어 시큐리티 컨텍스트를 채우는 필터.
 *
 * <p><b>토큰이 없거나 유효하지 않아도 예외를 던지지 않는다.</b> 그냥 익명으로 통과시키고, 접근 제어는
 * {@code SecurityConfig}의 경로 규칙이 판단한다. 공개 엔드포인트에 만료된 토큰을 달고 와도 동작해야
 * 하기 때문이다.
 *
 * <p>JWT 검증은 순수 CPU 연산(서명 대조)이라 이벤트 루프에서 수행해도 블로킹이 아니다 — DB나 Redis를
 * 조회하지 않는 것이 이 설계의 핵심이다.
 */
@Component
public class JwtAuthenticationWebFilter implements WebFilter {

    private static final String HEADER = "Authorization";
    private static final String PREFIX = "Bearer ";

    private final TokenProviderPort tokenProvider;

    public JwtAuthenticationWebFilter(TokenProviderPort tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String token = extractToken(exchange);
        if (token == null) {
            return chain.filter(exchange);
        }

        return tokenProvider.parseAccessToken(token)
                .map(this::toAuthentication)
                .map(authentication -> chain.filter(exchange).contextWrite(
                        ReactiveSecurityContextHolder.withAuthentication(authentication)))
                .orElseGet(() -> chain.filter(exchange));
    }

    private String extractToken(ServerWebExchange exchange) {
        String header = exchange.getRequest().getHeaders().getFirst(HEADER);
        if (header == null || !header.startsWith(PREFIX)) {
            return null;
        }
        String token = header.substring(PREFIX.length()).trim();
        return token.isEmpty() ? null : token;
    }

    /** 주체 이름을 userId로 둔다. 컨트롤러는 {@code Principal.getName()}으로 이 값을 읽는다. */
    private Authentication toAuthentication(AuthenticatedUser user) {
        List<SimpleGrantedAuthority> authorities =
                List.of(new SimpleGrantedAuthority("ROLE_" + user.role()));
        return new UsernamePasswordAuthenticationToken(user.userId(), null, authorities);
    }
}
