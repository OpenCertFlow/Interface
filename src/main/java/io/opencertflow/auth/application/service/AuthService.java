package io.opencertflow.auth.application.service;

import io.opencertflow.auth.application.port.in.LoginUseCase;
import io.opencertflow.auth.application.port.in.LogoutUseCase;
import io.opencertflow.auth.application.port.in.RefreshTokenUseCase;
import io.opencertflow.auth.application.port.in.SignUpUseCase;
import io.opencertflow.auth.application.port.out.LoadUserPort;
import io.opencertflow.auth.application.port.out.PasswordEncoderPort;
import io.opencertflow.auth.application.port.out.RefreshTokenStorePort;
import io.opencertflow.auth.application.port.out.SaveUserPort;
import io.opencertflow.auth.application.port.out.TermsPort;
import io.opencertflow.auth.application.port.out.TermsPort.AgreedTerm;
import io.opencertflow.auth.application.port.out.TermsPort.Term;
import io.opencertflow.auth.application.port.out.TokenProviderPort;
import io.opencertflow.auth.application.port.out.TokenProviderPort.AuthenticatedUser;
import io.opencertflow.auth.application.port.out.TokenProviderPort.IssuedTokens;
import io.opencertflow.auth.domain.error.AuthErrorCode;
import io.opencertflow.auth.domain.model.Email;
import io.opencertflow.auth.domain.model.Nickname;
import io.opencertflow.auth.domain.model.PasswordHash;
import io.opencertflow.auth.domain.model.User;
import io.opencertflow.auth.domain.model.UserId;
import io.opencertflow.common.application.annotation.UseCase;
import io.opencertflow.common.application.support.BlockingBridge;
import io.opencertflow.common.domain.error.BusinessException;
import io.opencertflow.common.domain.port.IdGenerator;
import io.opencertflow.common.domain.port.TimeProvider;
import java.util.List;
import java.util.Set;
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
    private final TermsPort termsPort;
    private final BlockingBridge blockingBridge;
    private final IdGenerator idGenerator;
    private final TimeProvider timeProvider;

    public AuthService(
            LoadUserPort loadUserPort,
            SaveUserPort saveUserPort,
            PasswordEncoderPort passwordEncoder,
            TokenProviderPort tokenProvider,
            RefreshTokenStorePort refreshTokenStore,
            TermsPort termsPort,
            BlockingBridge blockingBridge,
            IdGenerator idGenerator,
            TimeProvider timeProvider) {
        this.loadUserPort = loadUserPort;
        this.saveUserPort = saveUserPort;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.refreshTokenStore = refreshTokenStore;
        this.termsPort = termsPort;
        this.blockingBridge = blockingBridge;
        this.idGenerator = idGenerator;
        this.timeProvider = timeProvider;
    }

    @Override
    public Mono<UserId> signUp(SignUpCommand command) {
        Email email = Email.of(command.email());
        Nickname nickname = Nickname.of(command.nickname());
        Set<String> agreedKeys = Set.copyOf(command.agreedTermKeys());

        return blockingBridge.mono(() -> {
            if (loadUserPort.existsByEmail(email)) {
                throw new BusinessException(AuthErrorCode.EMAIL_ALREADY_REGISTERED);
            }
            // 필수 약관에 모두 동의했는지 확인한다(F-AUTH-008). 하나라도 빠지면 가입을 막는다.
            List<Term> activeTerms = termsPort.loadActive();
            boolean allRequiredAgreed = activeTerms.stream()
                    .filter(Term::required)
                    .allMatch(term -> agreedKeys.contains(term.termKey()));
            if (!allRequiredAgreed) {
                throw new BusinessException(AuthErrorCode.TERMS_NOT_AGREED);
            }

            PasswordHash hash = passwordEncoder.encode(command.rawPassword());
            User user = User.registerLocal(
                    UserId.of(idGenerator.nextId()), email, hash, nickname, timeProvider.now());
            UserId savedId = saveUserPort.save(user).id();

            // 동의한 약관(필수+선택 중 동의한 것)을 기록한다.
            List<AgreedTerm> agreements = activeTerms.stream()
                    .filter(term -> agreedKeys.contains(term.termKey()))
                    .map(term -> new AgreedTerm(term.termKey(), term.version()))
                    .toList();
            termsPort.saveAgreements(savedId.value(), agreements, timeProvider.now());
            return savedId;
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
                // 재발급 시 옛 토큰은 즉시 폐기한다(회전). 탈취된 토큰이 무한정 재사용되지 않도록.
                .flatMap(valid -> refreshTokenStore
                        .delete(principal.userId(), command.refreshToken())
                        .then(loadById(UserId.of(Long.parseLong(principal.userId())))))
                .flatMap(this::issueAndStore);
    }

    @Override
    public Mono<Void> logout(String userId, String refreshToken) {
        return refreshTokenStore.delete(userId, refreshToken);
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
