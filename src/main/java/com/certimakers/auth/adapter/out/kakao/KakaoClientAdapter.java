package com.certimakers.auth.adapter.out.kakao;

import com.certimakers.auth.application.port.out.KakaoClientPort;
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
 * {@link KakaoClientPort}의 WebClient 구현. 논블로킹이다.
 *
 * <p>두 단계를 감춘다: (1) 인가 코드 → 액세스 토큰(카카오 토큰 엔드포인트), (2) 액세스 토큰 →
 * 사용자 정보(카카오 API). 애플리케이션은 "코드를 주면 프로필이 온다"만 안다.
 *
 * <p>카카오 호출 실패는 {@link AuthErrorCode#KAKAO_AUTH_FAILED}로 바꿔, 외부 시스템의 오류 형식이
 * 우리 응답으로 새어 나가지 않게 한다.
 */
@Component
public class KakaoClientAdapter implements KakaoClientPort {

    private final WebClient webClient;
    private final AuthProperties.Kakao config;

    public KakaoClientAdapter(WebClient.Builder webClientBuilder, AuthProperties properties) {
        this.webClient = webClientBuilder.build();
        this.config = properties.kakao();
    }

    @Override
    public Mono<KakaoProfile> fetchProfile(String authorizationCode) {
        return requestAccessToken(authorizationCode)
                .flatMap(this::requestProfile)
                .onErrorMap(error -> !(error instanceof BusinessException),
                        error -> new BusinessException(AuthErrorCode.KAKAO_AUTH_FAILED,
                                AuthErrorCode.KAKAO_AUTH_FAILED.defaultMessage(), Map.of(), error));
    }

    private Mono<String> requestAccessToken(String authorizationCode) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", config.clientId());
        form.add("redirect_uri", config.redirectUri());
        form.add("code", authorizationCode);
        if (config.clientSecret() != null && !config.clientSecret().isBlank()) {
            form.add("client_secret", config.clientSecret());
        }

        return webClient.post()
                .uri(config.tokenUri())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue(form)
                .retrieve()
                .bodyToMono(KakaoTokenResponse.class)
                .map(KakaoTokenResponse::accessToken);
    }

    private Mono<KakaoProfile> requestProfile(String accessToken) {
        return webClient.get()
                .uri(config.userInfoUri())
                .headers(headers -> headers.setBearerAuth(accessToken))
                .retrieve()
                .bodyToMono(KakaoUserResponse.class)
                .map(KakaoUserResponse::toProfile);
    }
}
