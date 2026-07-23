package com.certimakers.auth.application.port.out;

import com.certimakers.auth.domain.model.Email;

/**
 * 이메일 발송. 구현은 SMTP(JavaMailSender)다.
 *
 * <p>SMTP 전송은 블로킹이므로 호출자는 BlockingBridge로 감싼다. 발송 실패가 회원가입 전체를
 * 되돌리면 안 되는 경우(예: 인증 코드 재발송)는 호출자가 실패를 흡수한다.
 */
public interface SendEmailPort {

    void sendVerificationCode(Email to, String code);

    void sendPasswordResetLink(Email to, String resetToken);
}
