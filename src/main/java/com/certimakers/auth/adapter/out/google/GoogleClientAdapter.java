package com.certimakers.auth.adapter.out.google;

import com.certimakers.auth.application.port.out.GoogleClientPort;
import com.certimakers.auth.config.AuthProperties;
import com.certimakers.auth.domain.error.AuthErrorCode;
import com.certimakers.common.domain.error.BusinessException;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * {@link GoogleClientPort}의 WebClient 구현. 논블로킹이다({@code KakaoClientAdapter}과 같은 구조).
 *
 * <p>두 단계를 감춘다: (1) 인가 코드 → 액세스 토큰(구글 토큰 엔드포인트), (2) 액세스 토큰 →
 * 사용자 정보(OIDC userinfo). 애플리케이션은 "코드를 주면 프로필이 온다"만 안다.
 *
 * <p>구글 호출 실패는 {@link AuthErrorCode#GOOGLE_AUTH_FAILED}로 바꿔, 외부 시스템의 오류 형식이
 * 우리 응답으로 새어 나가지 않게 한다.
 */
@Component
public class GoogleClientAdapter implements GoogleClientPort {

    private final WebClient webClient;
    private final AuthProperties.Google config;

    public GoogleClientAdapter(WebClient.Builder webClientBuilder, AuthProperties properties) {
        this.webClient = webClientBuilder.build();
        this.config = properties.google();
    }

    @Override
    public Mono<GoogleProfile> fetchProfile(String authorizationCode) {
        return requestAccessToken(authorizationCode)
                .flatMap(this::requestProfile)
                .onErrorMap(error -> !(error instanceof BusinessException),
                        error -> new BusinessException(AuthErrorCode.GOOGLE_AUTH_FAILED,
                                AuthErrorCode.GOOGLE_AUTH_FAILED.defaultMessage(), Map.of(), error));
    }

    private Mono<String> requestAccessToken(String authorizationCode) {
        // 구글은 카카오와 달리 client_secret이 필수다.
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", config.clientId());
        form.add("client_secret", config.clientSecret());
        form.add("redirect_uri", config.redirectUri());
        form.add("code", authorizationCode);

        return webClient.post()
                .uri(config.tokenUri())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue(form)
                .retrieve()
                .bodyToMono(GoogleTokenResponse.class)
                .map(GoogleTokenResponse::accessToken);
    }

    private Mono<GoogleProfile> requestProfile(String accessToken) {
        return webClient.get()
                .uri(config.userInfoUri())
                .headers(headers -> headers.setBearerAuth(accessToken))
                .retrieve()
                .bodyToMono(GoogleUserResponse.class)
                .map(GoogleUserResponse::toProfile);
    }
}
