package io.opencertflow.auth.adapter.out.mail;

import io.opencertflow.auth.application.port.out.SendEmailPort;
import io.opencertflow.auth.config.AuthProperties;
import io.opencertflow.auth.domain.model.Email;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.MailSender;
import org.springframework.stereotype.Component;

/**
 * {@link SendEmailPort}의 SMTP 구현. {@link MailSender#send}는 블로킹이므로 호출자가
 * BlockingBridge로 감싼다.
 *
 * <p>본선 데모에서는 실제 SMTP 서버 대신 개발용 발신자로 동작할 수 있다. 텍스트 본문만 보내는
 * 최소 구현이며, 필요 시 HTML 템플릿으로 확장한다.
 */
@Component
public class SmtpSendEmailAdapter implements SendEmailPort {

    private final MailSender mailSender;
    private final String from;
    private final String resetLinkBaseUrl;

    public SmtpSendEmailAdapter(MailSender mailSender, AuthProperties properties) {
        this.mailSender = mailSender;
        this.from = properties.email().from();
        this.resetLinkBaseUrl = properties.email().resetLinkBaseUrl();
    }

    @Override
    public void sendVerificationCode(Email to, String code) {
        SimpleMailMessage message = baseMessage(to.value());
        message.setSubject("[OpenCertFlow] 이메일 인증 코드");
        message.setText("인증 코드는 %s 입니다. 5분 안에 입력해 주세요.".formatted(code));
        mailSender.send(message);
    }

    @Override
    public void sendPasswordResetLink(Email to, String resetToken) {
        SimpleMailMessage message = baseMessage(to.value());
        message.setSubject("[OpenCertFlow] 비밀번호 재설정");
        message.setText("아래 링크에서 비밀번호를 재설정해 주세요.\n%s?token=%s"
                .formatted(resetLinkBaseUrl, resetToken));
        mailSender.send(message);
    }

    private SimpleMailMessage baseMessage(String to) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        return message;
    }
}
