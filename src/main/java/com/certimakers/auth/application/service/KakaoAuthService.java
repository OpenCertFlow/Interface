package com.certimakers.auth.application.service;

import com.certimakers.auth.application.port.in.KakaoLoginUseCase;
import com.certimakers.auth.application.port.out.KakaoClientPort;
import com.certimakers.auth.application.port.out.KakaoClientPort.KakaoProfile;
import com.certimakers.auth.application.port.out.LoadUserPort;
import com.certimakers.auth.application.port.out.RefreshTokenStorePort;
import com.certimakers.auth.application.port.out.SaveUserPort;
import com.certimakers.auth.application.port.out.TokenProviderPort;
import com.certimakers.auth.application.port.out.TokenProviderPort.IssuedTokens;
import com.certimakers.auth.domain.model.Email;
import com.certimakers.auth.domain.model.Nickname;
import com.certimakers.auth.domain.model.User;
import com.certimakers.auth.domain.model.UserId;
import com.certimakers.common.application.annotation.UseCase;
import com.certimakers.common.application.support.BlockingBridge;
import com.certimakers.common.domain.port.IdGenerator;
import com.certimakers.common.domain.port.TimeProvider;
import reactor.core.publisher.Mono;

/**
 * 카카오 로그인 오케스트레이션: 인가 코드 → 프로필 조회(WebClient, 논블로킹) → 계정 매칭/생성 →
 * 토큰 발급.
 *
 * <p>기존 계정이면 로그인, 없으면 즉시 가입시킨다. 소셜 로그인은 "가입"과 "로그인"을 사용자에게
 * 구분해 보여주지 않는 것이 관례다.
 *
 * <p>이메일 제공에 동의하지 않은 카카오 사용자는 이메일이 없다. 그 경우 회원번호 기반의 대체
 * 이메일을 만들어 계정 유일성을 보장한다 — 실제 발송용이 아니라 식별용 값이다.
 */
@UseCase
public class KakaoAuthService implements KakaoLoginUseCase {

    private final KakaoClientPort kakaoClient;
    private final LoadUserPort loadUserPort;
    private final SaveUserPort saveUserPort;
    private final TokenProviderPort tokenProvider;
    private final RefreshTokenStorePort refreshTokenStore;
    private final BlockingBridge blockingBridge;
    private final IdGenerator idGenerator;
    private final TimeProvider timeProvider;

    public KakaoAuthService(
            KakaoClientPort kakaoClient,
            LoadUserPort loadUserPort,
            SaveUserPort saveUserPort,
            TokenProviderPort tokenProvider,
            RefreshTokenStorePort refreshTokenStore,
            BlockingBridge blockingBridge,
            IdGenerator idGenerator,
            TimeProvider timeProvider) {
        this.kakaoClient = kakaoClient;
        this.loadUserPort = loadUserPort;
        this.saveUserPort = saveUserPort;
        this.tokenProvider = tokenProvider;
        this.refreshTokenStore = refreshTokenStore;
        this.blockingBridge = blockingBridge;
        this.idGenerator = idGenerator;
        this.timeProvider = timeProvider;
    }

    @Override
    public Mono<IssuedTokens> login(KakaoLoginCommand command) {
        return kakaoClient.fetchProfile(command.authorizationCode())
                .flatMap(profile -> blockingBridge.mono(() -> findOrCreate(profile)))
                .flatMap(this::issueAndStore);
    }

    private User findOrCreate(KakaoProfile profile) {
        return loadUserPort.findByKakaoId(profile.kakaoId())
                .orElseGet(() -> saveUserPort.save(newKakaoUser(profile)));
    }

    private User newKakaoUser(KakaoProfile profile) {
        Email email = Email.of(resolveEmail(profile));
        Nickname nickname = Nickname.of(resolveNickname(profile));
        return User.registerKakao(
                UserId.of(idGenerator.nextId()), email, nickname, profile.kakaoId(), timeProvider.now());
    }

    /** 이메일 미동의 시 회원번호 기반 식별용 주소를 만든다. 유일성 보장이 목적이다. */
    private String resolveEmail(KakaoProfile profile) {
        if (profile.email() != null && !profile.email().isBlank()) {
            return profile.email();
        }
        return "kakao_" + profile.kakaoId() + "@kakao.certimakers.local";
    }

    private String resolveNickname(KakaoProfile profile) {
        if (profile.nickname() != null && !profile.nickname().isBlank()) {
            return profile.nickname();
        }
        return "카카오사용자";
    }

    private Mono<IssuedTokens> issueAndStore(User user) {
        IssuedTokens tokens = tokenProvider.issue(user);
        return refreshTokenStore.save(user.id().value().toString(), tokens.refreshToken())
                .thenReturn(tokens);
    }
}
