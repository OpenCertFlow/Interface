package com.certimakers.auth.config;

import com.certimakers.common.adapter.in.web.response.ApiResponse;
import com.certimakers.common.adapter.in.web.response.ErrorResponse;
import com.certimakers.auth.domain.error.AuthErrorCode;
import com.certimakers.common.domain.port.TimeProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 리액티브 시큐리티 구성. 서블릿 필터 체인이 아니라 {@link SecurityWebFilterChain}을 쓴다 —
 * 이 애플리케이션은 WebFlux이며 MVC 스택과 공존할 수 없다.
 *
 * <p>진단·컨설팅 등 기존 API는 인증 없이 쓰도록 열어 둔다. 해커톤 시연 흐름(앱 설치 후 즉시 진단)이
 * 로그인 벽에 막히면 안 되기 때문이다. 마이페이지({@code /api/v1/me/**})만 인증을 요구한다.
 *
 * <p>세션·CSRF·기본 로그인 폼을 모두 끈다. JWT 기반 무상태 API이므로 필요 없고, 켜 두면 오히려
 * 클라이언트가 예상하지 못한 302·403을 받는다.
 */
@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationWebFilter jwtFilter;
    private final ObjectMapper objectMapper;
    private final TimeProvider timeProvider;

    public SecurityConfig(
            JwtAuthenticationWebFilter jwtFilter,
            ObjectMapper objectMapper,
            TimeProvider timeProvider) {
        this.jwtFilter = jwtFilter;
        this.objectMapper = objectMapper;
        this.timeProvider = timeProvider;
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .logout(ServerHttpSecurity.LogoutSpec::disable)
                .securityContextRepository(
                        org.springframework.security.web.server.context.NoOpServerSecurityContextRepository
                                .getInstance())
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // 인증 없이 쓰는 기존 서비스 — 시연 흐름을 막지 않는다
                        .pathMatchers("/api/v1/auth/**").permitAll()
                        .pathMatchers("/api/v1/diagnoses/**").permitAll()
                        .pathMatchers("/api/v1/consulting-leads/**").permitAll()
                        .pathMatchers("/api/v1/product-groups/**").permitAll()
                        .pathMatchers("/actuator/**").permitAll()
                        .pathMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        // 게시판 열람과 파일 다운로드는 비로그인도 가능하다.
                        // 비밀글 가리기는 도메인이 하고, 여기서는 경로만 연다.
                        .pathMatchers(HttpMethod.GET, "/api/v1/boards/**").permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/v1/files/**").permitAll()
                        // 어떤 문서를 만들 수 있는지는 로그인 전에도 보여 준다
                        .pathMatchers(HttpMethod.GET, "/api/v1/documents/templates").permitAll()
                        // 관리자 전용. 권한 판단을 여기 한 곳에만 둔다
                        .pathMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        // 마이페이지·글쓰기·파일 업로드·문서 발급은 인증이 필요하다
                        .pathMatchers("/api/v1/me/**").authenticated()
                        .pathMatchers("/api/v1/boards/**").authenticated()
                        .pathMatchers("/api/v1/files/**").authenticated()
                        .pathMatchers("/api/v1/documents/**").authenticated()
                        .anyExchange().permitAll())
                .addFilterAt(jwtFilter, org.springframework.security.config.web.server.SecurityWebFiltersOrder.AUTHENTICATION)
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint((exchange, error) ->
                                writeError(exchange, HttpStatus.UNAUTHORIZED, AuthErrorCode.UNAUTHENTICATED))
                        .accessDeniedHandler((exchange, error) ->
                                writeError(exchange, HttpStatus.FORBIDDEN, AuthErrorCode.UNAUTHENTICATED)))
                .build();
    }

    /**
     * 인증 실패도 {@code ApiResponse} 봉투로 돌려준다.
     *
     * <p>시큐리티는 컨트롤러 앞단에서 끊기므로 {@code GlobalExceptionHandler}가 잡지 못한다. 여기서
     * 같은 형태로 응답하지 않으면 클라이언트가 인증 실패에서만 다른 파싱 경로를 타야 한다.
     */
    private Mono<Void> writeError(
            ServerWebExchange exchange, HttpStatus status, AuthErrorCode errorCode) {

        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String traceId = exchange.getResponse().getHeaders()
                .getFirst(com.certimakers.common.adapter.in.web.trace.TraceId.HEADER);
        ApiResponse<Void> body = ApiResponse.failure(
                ErrorResponse.of(errorCode.code(), errorCode.defaultMessage()),
                traceId != null ? traceId : com.certimakers.common.adapter.in.web.trace.TraceId.UNKNOWN,
                timeProvider.now());

        try {
            DataBuffer buffer = exchange.getResponse().bufferFactory()
                    .wrap(objectMapper.writeValueAsBytes(body));
            return exchange.getResponse().writeWith(Mono.just(buffer));
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            return exchange.getResponse().setComplete();
        }
    }
}
