package io.opencertflow.auth.application.service;

import io.opencertflow.auth.application.port.in.EmailVerificationUseCase;
import io.opencertflow.auth.application.port.out.AttemptLimiterPort;
import io.opencertflow.auth.application.port.out.AttemptLimiterPort.Limit;
import io.opencertflow.auth.application.port.out.LoadUserPort;
import io.opencertflow.auth.application.port.out.SaveUserPort;
import io.opencertflow.auth.application.port.out.SendEmailPort;
import io.opencertflow.auth.application.port.out.VerificationCodeGeneratorPort;
import io.opencertflow.auth.application.port.out.VerificationCodeStorePort;
import io.opencertflow.auth.domain.error.AuthErrorCode;
import io.opencertflow.auth.domain.model.Email;
import io.opencertflow.auth.domain.model.User;
import io.opencertflow.common.application.annotation.UseCase;
import io.opencertflow.common.application.support.BlockingBridge;
import io.opencertflow.common.domain.error.BusinessException;
import reactor.core.publisher.Mono;

/**
 * 이메일 인증 오케스트레이션. 코드는 Redis(논블로킹)에 TTL과 함께 저장하고, 발송은 SMTP(블로킹)라
 * BlockingBridge로 감싼다.
 *
 * <p>코드 생성을 {@link VerificationCodeGeneratorPort}로 밀어낸 덕분에 이 서비스에는 난수가 없다 —
 * 테스트에서 코드를 고정해 발송·저장·검증 흐름을 결정적으로 확인할 수 있다.
 */
@UseCase
public class EmailVerificationService implements EmailVerificationUseCase {

    private final VerificationCodeStorePort codeStore;
    private final VerificationCodeGeneratorPort codeGenerator;
    private final SendEmailPort sendEmailPort;
    private final LoadUserPort loadUserPort;
    private final SaveUserPort saveUserPort;
    private final AttemptLimiterPort attemptLimiter;
    private final BlockingBridge blockingBridge;

    public EmailVerificationService(
            VerificationCodeStorePort codeStore,
            VerificationCodeGeneratorPort codeGenerator,
            SendEmailPort sendEmailPort,
            LoadUserPort loadUserPort,
            SaveUserPort saveUserPort,
            AttemptLimiterPort attemptLimiter,
            BlockingBridge blockingBridge) {
        this.codeStore = codeStore;
        this.codeGenerator = codeGenerator;
        this.sendEmailPort = sendEmailPort;
        this.loadUserPort = loadUserPort;
        this.saveUserPort = saveUserPort;
        this.attemptLimiter = attemptLimiter;
        this.blockingBridge = blockingBridge;
    }

    /** 발송도 센다. 제한이 없으면 남의 주소로 메일 폭탄을 보낼 수 있고 발송 비용도 우리가 문다. */
    @Override
    public Mono<Void> sendCode(String rawEmail) {
        Email email = Email.of(rawEmail);
        String code = codeGenerator.newNumericCode();

        return rejectIfTooMany("email-send:" + email.value(), Limit.EMAIL_CODE_SEND)
                .then(codeStore.save(email, code))
                .then(blockingBridge.run(() -> sendEmailPort.sendVerificationCode(email, code)));
    }

    /**
     * 코드 검증.
     *
     * <p><b>시도 횟수를 세지 않으면 6자리 코드는 비밀이 아니다.</b> 후보가 100만 개뿐이라 유효
     * 시간(5분) 안에 전수 탐색이 끝난다. 코드를 길게 만드는 것보다 시도를 세는 쪽이 본질이다.
     *
     * <p>성공하면 카운터를 지운다. 오타를 몇 번 낸 정상 사용자가 그 뒤로 막히면 안 된다.
     */
    @Override
    public Mono<Void> verify(VerifyEmailCommand command) {
        Email email = Email.of(command.email());
        String limiterKey = "email-verify:" + email.value();

        return rejectIfTooMany(limiterKey, Limit.EMAIL_VERIFICATION)
                .then(codeStore.matches(email, command.code()))
                .filter(Boolean::booleanValue)
                .switchIfEmpty(Mono.error(
                        new BusinessException(AuthErrorCode.EMAIL_VERIFICATION_FAILED)))
                .then(attemptLimiter.reset(limiterKey))
                .then(codeStore.delete(email))
                .then(markVerified(email));
    }

    private Mono<Void> rejectIfTooMany(String key, Limit limit) {
        return attemptLimiter.exceeded(key, limit)
                .filter(Boolean::booleanValue)
                .flatMap(exceeded -> Mono.<Void>error(
                        new BusinessException(AuthErrorCode.TOO_MANY_ATTEMPTS)))
                .then();
    }

    /** 코드 검증 성공 후 사용자 애그리거트의 이메일 인증 플래그를 올린다. */
    private Mono<Void> markVerified(Email email) {
        return blockingBridge.mono(() -> {
            User user = loadUserPort.findByEmail(email)
                    .orElseThrow(() -> new BusinessException(AuthErrorCode.USER_NOT_FOUND));
            user.verifyEmail();
            saveUserPort.save(user);
            return true;
        }).then();
    }
}
