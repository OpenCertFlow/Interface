package com.certimakers.auth.application.service;

import com.certimakers.auth.application.port.in.LoginUseCase;
import com.certimakers.auth.application.port.in.LogoutUseCase;
import com.certimakers.auth.application.port.in.RefreshTokenUseCase;
import com.certimakers.auth.application.port.in.SignUpUseCase;
import com.certimakers.auth.application.port.out.LoadUserPort;
import com.certimakers.auth.application.port.out.PasswordEncoderPort;
import com.certimakers.auth.application.port.out.RefreshTokenStorePort;
import com.certimakers.auth.application.port.out.SaveUserPort;
import com.certimakers.auth.application.port.out.TokenProviderPort;
import com.certimakers.auth.application.port.out.TokenProviderPort.AuthenticatedUser;
import com.certimakers.auth.application.port.out.TokenProviderPort.IssuedTokens;
import com.certimakers.auth.domain.error.AuthErrorCode;
import com.certimakers.auth.domain.model.Email;
import com.certimakers.auth.domain.model.Nickname;
import com.certimakers.auth.domain.model.PasswordHash;
import com.certimakers.auth.domain.model.User;
import com.certimakers.auth.domain.model.UserId;
import com.certimakers.common.application.annotation.UseCase;
import com.certimakers.common.application.support.BlockingBridge;
import com.certimakers.common.domain.error.BusinessException;
import com.certimakers.common.domain.port.IdGenerator;
import com.certimakers.common.domain.port.TimeProvider;
import reactor.core.publisher.Mono;

/**
 * 이메일·비밀번호 인증 오케스트레이션: 회원가입·로그인·토큰 재발급·로그아웃.
 *
 * <p>블로킹 자원(JPA 조회·저장, BCrypt 해싱·대조)은 {@link BlockingBridge}로 감싸 이벤트 루프 밖에서
 * 돌린다. 리프레시 토큰 저장소는 Redis(논블로킹)라 그대로 체인에 잇는다.
 */
@UseCase
public class AuthService implements SignUpUseCase, LoginUseCase, RefreshTokenUseCase, LogoutUseCase {

    private final LoadUserPort loadUserPort;
    private final SaveUserPort saveUserPort;
    private final PasswordEncoderPort passwordEncoder;
    private final TokenProviderPort tokenProvider;
    private final RefreshTokenStorePort refreshTokenStore;
    private final BlockingBridge blockingBridge;
    private final IdGenerator idGenerator;
    private final TimeProvider timeProvider;

    public AuthService(
            LoadUserPort loadUserPort,
            SaveUserPort saveUserPort,
            PasswordEncoderPort passwordEncoder,
            TokenProviderPort tokenProvider,
            RefreshTokenStorePort refreshTokenStore,
            BlockingBridge blockingBridge,
            IdGenerator idGenerator,
            TimeProvider timeProvider) {
        this.loadUserPort = loadUserPort;
        this.saveUserPort = saveUserPort;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.refreshTokenStore = refreshTokenStore;
        this.blockingBridge = blockingBridge;
        this.idGenerator = idGenerator;
        this.timeProvider = timeProvider;
    }

    @Override
    public Mono<UserId> signUp(SignUpCommand command) {
        Email email = Email.of(command.email());
        Nickname nickname = Nickname.of(command.nickname());

        return blockingBridge.mono(() -> {
            if (loadUserPort.existsByEmail(email)) {
                throw new BusinessException(AuthErrorCode.EMAIL_ALREADY_REGISTERED);
            }
            PasswordHash hash = passwordEncoder.encode(command.rawPassword());
            User user = User.registerLocal(
                    UserId.of(idGenerator.nextId()), email, hash, nickname, timeProvider.now());
            return saveUserPort.save(user).id();
        });
    }

    @Override
    public Mono<IssuedTokens> login(LoginCommand command) {
        Email email = Email.of(command.email());

        return blockingBridge.mono(() -> {
            // 이메일 부재와 비밀번호 불일치를 같은 오류로 응답한다 — 어느 쪽인지 알려주면 가입 여부가 샌다.
            User user = loadUserPort.findByEmail(email)
                    .orElseThrow(() -> new BusinessException(AuthErrorCode.INVALID_CREDENTIALS));
            PasswordHash hash = user.passwordHashForVerification();
            if (!passwordEncoder.matches(command.rawPassword(), hash)) {
                throw new BusinessException(AuthErrorCode.INVALID_CREDENTIALS);
            }
            return user;
        }).flatMap(this::issueAndStore);
    }

    @Override
    public Mono<IssuedTokens> refresh(RefreshCommand command) {
        AuthenticatedUser principal = tokenProvider.parseAccessToken(command.refreshToken())
                .orElseThrow(() -> new BusinessException(AuthErrorCode.INVALID_REFRESH_TOKEN));

        return refreshTokenStore.matches(principal.userId(), command.refreshToken())
                .filter(Boolean::booleanValue)
                .switchIfEmpty(Mono.error(new BusinessException(AuthErrorCode.INVALID_REFRESH_TOKEN)))
                .flatMap(valid -> loadById(UserId.of(java.util.UUID.fromString(principal.userId()))))
                .flatMap(this::issueAndStore);
    }

    @Override
    public Mono<Void> logout(String userId) {
        return refreshTokenStore.delete(userId);
    }

    /** 토큰을 발급하고 리프레시 토큰을 저장소에 심는다. 이후 이 리프레시 토큰만 재발급에 쓸 수 있다. */
    private Mono<IssuedTokens> issueAndStore(User user) {
        IssuedTokens tokens = tokenProvider.issue(user);
        return refreshTokenStore.save(user.id().value().toString(), tokens.refreshToken())
                .thenReturn(tokens);
    }

    private Mono<User> loadById(UserId id) {
        return blockingBridge.mono(() -> loadUserPort.findById(id)
                .orElseThrow(() -> new BusinessException(AuthErrorCode.USER_NOT_FOUND)));
    }
}
