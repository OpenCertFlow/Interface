package io.opencertflow.auth.application.service;

import io.opencertflow.auth.application.port.in.EmailVerificationUseCase;
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
    private final BlockingBridge blockingBridge;

    public EmailVerificationService(
            VerificationCodeStorePort codeStore,
            VerificationCodeGeneratorPort codeGenerator,
            SendEmailPort sendEmailPort,
            LoadUserPort loadUserPort,
            SaveUserPort saveUserPort,
            BlockingBridge blockingBridge) {
        this.codeStore = codeStore;
        this.codeGenerator = codeGenerator;
        this.sendEmailPort = sendEmailPort;
        this.loadUserPort = loadUserPort;
        this.saveUserPort = saveUserPort;
        this.blockingBridge = blockingBridge;
    }

    @Override
    public Mono<Void> sendCode(String rawEmail) {
        Email email = Email.of(rawEmail);
        String code = codeGenerator.newNumericCode();

        return codeStore.save(email, code)
                .then(blockingBridge.run(() -> sendEmailPort.sendVerificationCode(email, code)));
    }

    @Override
    public Mono<Void> verify(VerifyEmailCommand command) {
        Email email = Email.of(command.email());

        return codeStore.matches(email, command.code())
                .filter(Boolean::booleanValue)
                .switchIfEmpty(Mono.error(
                        new BusinessException(AuthErrorCode.EMAIL_VERIFICATION_FAILED)))
                .then(codeStore.delete(email))
                .then(markVerified(email));
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
