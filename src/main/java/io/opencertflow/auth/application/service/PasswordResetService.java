package io.opencertflow.auth.application.service;

import io.opencertflow.auth.application.port.in.PasswordResetUseCase;
import io.opencertflow.auth.application.port.out.AttemptLimiterPort;
import io.opencertflow.auth.application.port.out.AttemptLimiterPort.Limit;
import io.opencertflow.auth.application.port.out.LoadUserPort;
import io.opencertflow.auth.application.port.out.PasswordEncoderPort;
import io.opencertflow.auth.application.port.out.PasswordResetTokenStorePort;
import io.opencertflow.auth.application.port.out.SaveUserPort;
import io.opencertflow.auth.application.port.out.SendEmailPort;
import io.opencertflow.auth.application.port.out.VerificationCodeGeneratorPort;
import io.opencertflow.auth.domain.error.AuthErrorCode;
import io.opencertflow.auth.domain.model.Email;
import io.opencertflow.auth.domain.model.PasswordHash;
import io.opencertflow.auth.domain.model.User;
import io.opencertflow.common.application.annotation.UseCase;
import io.opencertflow.common.application.support.BlockingBridge;
import io.opencertflow.common.domain.error.BusinessException;
import reactor.core.publisher.Mono;

/**
 * 비밀번호 찾기 오케스트레이션.
 *
 * <p>재설정 요청은 <b>계정 존재 여부를 응답으로 노출하지 않는다.</b> 가입되지 않은 이메일이어도
 * 성공으로 응답하고, 실제 계정이 있을 때만 토큰을 저장·발송한다. 소셜 계정도 비밀번호가 없으므로
 * 조용히 넘어간다 — 어느 경우든 외부에서 보이는 응답은 동일하다.
 */
@UseCase
public class PasswordResetService implements PasswordResetUseCase {

    private final PasswordResetTokenStorePort tokenStore;
    private final VerificationCodeGeneratorPort codeGenerator;
    private final SendEmailPort sendEmailPort;
    private final LoadUserPort loadUserPort;
    private final SaveUserPort saveUserPort;
    private final PasswordEncoderPort passwordEncoder;
    private final AttemptLimiterPort attemptLimiter;
    private final BlockingBridge blockingBridge;

    public PasswordResetService(
            PasswordResetTokenStorePort tokenStore,
            VerificationCodeGeneratorPort codeGenerator,
            SendEmailPort sendEmailPort,
            LoadUserPort loadUserPort,
            SaveUserPort saveUserPort,
            PasswordEncoderPort passwordEncoder,
            AttemptLimiterPort attemptLimiter,
            BlockingBridge blockingBridge) {
        this.tokenStore = tokenStore;
        this.codeGenerator = codeGenerator;
        this.sendEmailPort = sendEmailPort;
        this.loadUserPort = loadUserPort;
        this.saveUserPort = saveUserPort;
        this.passwordEncoder = passwordEncoder;
        this.attemptLimiter = attemptLimiter;
        this.blockingBridge = blockingBridge;
    }

    /**
     * 재설정 링크 발송 요청.
     *
     * <p>제한을 두는 이유는 두 가지다. 남의 주소로 메일을 반복 발송하는 괴롭힘을 막고, 재설정
     * 토큰이 짧은 시간에 여러 개 유효해지는 상태를 피한다.
     */
    @Override
    public Mono<Void> requestReset(String rawEmail) {
        Email email = Email.of(rawEmail);

        return attemptLimiter.exceeded("password-reset:" + email.value(), Limit.PASSWORD_RESET)
                .filter(Boolean::booleanValue)
                .flatMap(exceeded -> Mono.<java.util.Optional<User>>error(
                        new BusinessException(AuthErrorCode.TOO_MANY_ATTEMPTS)))
                .switchIfEmpty(blockingBridge.mono(() -> loadUserPort.findByEmail(email)))
                // 로컬 계정만 재설정 대상. 소셜 계정·미가입은 조용히 통과시켜 존재 여부를 숨긴다.
                .filter(found -> found.isPresent() && !found.get().provider().isSocial())
                .flatMap(found -> {
                    String token = codeGenerator.newResetToken();
                    return tokenStore.save(token, email)
                            .then(blockingBridge.run(() -> sendEmailPort.sendPasswordResetLink(email, token)));
                })
                // 대상이 없어도(위 filter가 비면) 성공으로 끝낸다 — 계정 열거를 막는다.
                .then();
    }

    @Override
    public Mono<Void> reset(ResetPasswordCommand command) {
        return tokenStore.findEmail(command.token())
                .flatMap(maybeEmail -> maybeEmail
                        .map(email -> applyNewPassword(email, command.newRawPassword())
                                .then(tokenStore.delete(command.token())))
                        .orElseGet(() -> Mono.error(
                                new BusinessException(AuthErrorCode.PASSWORD_RESET_TOKEN_INVALID))));
    }

    private Mono<Void> applyNewPassword(Email email, String newRawPassword) {
        return blockingBridge.mono(() -> {
            User user = loadUserPort.findByEmail(email)
                    .orElseThrow(() -> new BusinessException(AuthErrorCode.USER_NOT_FOUND));
            PasswordHash newHash = passwordEncoder.encode(newRawPassword);
            user.changePassword(newHash); // 소셜 계정이면 도메인이 여기서 거부
            saveUserPort.save(user);
            return true;
        }).then();
    }
}
