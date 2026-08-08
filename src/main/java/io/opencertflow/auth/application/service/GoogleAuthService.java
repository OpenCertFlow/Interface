package io.opencertflow.auth.application.service;

import io.opencertflow.auth.application.port.in.GoogleLoginUseCase;
import io.opencertflow.auth.application.port.out.GoogleClientPort;
import io.opencertflow.auth.application.port.out.GoogleClientPort.GoogleProfile;
import io.opencertflow.auth.application.port.out.LoadUserPort;
import io.opencertflow.auth.application.port.out.RefreshTokenStorePort;
import io.opencertflow.auth.application.port.out.SaveUserPort;
import io.opencertflow.auth.application.port.out.TokenProviderPort;
import io.opencertflow.auth.application.port.out.TokenProviderPort.IssuedTokens;
import io.opencertflow.auth.domain.model.Email;
import io.opencertflow.auth.domain.model.Nickname;
import io.opencertflow.auth.domain.model.User;
import io.opencertflow.auth.domain.model.UserId;
import io.opencertflow.common.application.annotation.UseCase;
import io.opencertflow.common.application.support.BlockingBridge;
import io.opencertflow.common.domain.port.IdGenerator;
import io.opencertflow.common.domain.port.TimeProvider;
import reactor.core.publisher.Mono;

/**
 * 구글 로그인 오케스트레이션: 인가 코드 → 프로필 조회(WebClient, 논블로킹) → 계정 매칭/생성 →
 * 토큰 발급. {@link KakaoAuthService}와 같은 흐름이다.
 *
 * <p>기존 계정이면 로그인, 없으면 즉시 가입시킨다. 소셜 로그인은 "가입"과 "로그인"을 사용자에게
 * 구분해 보여주지 않는 것이 관례다.
 *
 * <p>이메일이 없는 경우(범위 미동의 등)는 식별자 기반의 대체 이메일을 만들어 계정 유일성을
 * 보장한다 — 실제 발송용이 아니라 식별용 값이다.
 */
@UseCase
public class GoogleAuthService implements GoogleLoginUseCase {

    private final GoogleClientPort googleClient;
    private final LoadUserPort loadUserPort;
    private final SaveUserPort saveUserPort;
    private final TokenProviderPort tokenProvider;
    private final RefreshTokenStorePort refreshTokenStore;
    private final BlockingBridge blockingBridge;
    private final IdGenerator idGenerator;
    private final TimeProvider timeProvider;

    public GoogleAuthService(
            GoogleClientPort googleClient,
            LoadUserPort loadUserPort,
            SaveUserPort saveUserPort,
            TokenProviderPort tokenProvider,
            RefreshTokenStorePort refreshTokenStore,
            BlockingBridge blockingBridge,
            IdGenerator idGenerator,
            TimeProvider timeProvider) {
        this.googleClient = googleClient;
        this.loadUserPort = loadUserPort;
        this.saveUserPort = saveUserPort;
        this.tokenProvider = tokenProvider;
        this.refreshTokenStore = refreshTokenStore;
        this.blockingBridge = blockingBridge;
        this.idGenerator = idGenerator;
        this.timeProvider = timeProvider;
    }

    @Override
    public Mono<IssuedTokens> login(GoogleLoginCommand command) {
        return googleClient.fetchProfile(command.authorizationCode())
                .flatMap(profile -> blockingBridge.mono(() -> findOrCreate(profile)))
                .flatMap(this::issueAndStore);
    }

    private User findOrCreate(GoogleProfile profile) {
        return loadUserPort.findByGoogleId(profile.googleId())
                .orElseGet(() -> saveUserPort.save(newGoogleUser(profile)));
    }

    private User newGoogleUser(GoogleProfile profile) {
        Email email = Email.of(resolveEmail(profile));
        Nickname nickname = Nickname.of(resolveNickname(profile));
        return User.registerGoogle(
                UserId.of(idGenerator.nextId()), email, nickname, profile.googleId(), timeProvider.now());
    }

    /** 이메일 미제공 시 식별자 기반 식별용 주소를 만든다. 유일성 보장이 목적이다. */
    private String resolveEmail(GoogleProfile profile) {
        if (profile.email() != null && !profile.email().isBlank()) {
            return profile.email();
        }
        return "google_" + profile.googleId() + "@google.opencertflow.local";
    }

    private String resolveNickname(GoogleProfile profile) {
        if (profile.name() != null && !profile.name().isBlank()) {
            return profile.name();
        }
        return "구글사용자";
    }

    private Mono<IssuedTokens> issueAndStore(User user) {
        IssuedTokens tokens = tokenProvider.issue(user);
        return refreshTokenStore.save(user.id().value().toString(), tokens.refreshToken())
                .thenReturn(tokens);
    }
}
