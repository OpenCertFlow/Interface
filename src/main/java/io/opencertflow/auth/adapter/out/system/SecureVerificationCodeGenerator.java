package io.opencertflow.auth.adapter.out.system;

import io.opencertflow.auth.application.port.out.VerificationCodeGeneratorPort;
import java.security.SecureRandom;
import java.util.Base64;
import org.springframework.stereotype.Component;

/**
 * {@link VerificationCodeGeneratorPort}의 보안 난수 구현.
 *
 * <p>{@link SecureRandom}은 리눅스에서 파일 읽기로 블로킹될 수 있다. 그러나 이 생성기는 이벤트
 * 루프가 아니라 <b>이메일 발송을 감싸는 블로킹 스케줄러 위</b>에서만 호출되므로 문제가 되지 않는다
 * (TraceIdFactory와 대비되는 지점 — 추적 ID는 매 요청 이벤트 루프에서 만들어 SecureRandom을 피했다).
 */
@Component
public class SecureVerificationCodeGenerator implements VerificationCodeGeneratorPort {

    private static final int RESET_TOKEN_BYTES = 32;

    private final SecureRandom random = new SecureRandom();
    private final Base64.Encoder urlEncoder = Base64.getUrlEncoder().withoutPadding();

    @Override
    public String newNumericCode() {
        int code = random.nextInt(1_000_000); // 0 ~ 999999
        return String.format("%06d", code);   // 6자리로 0 채움
    }

    @Override
    public String newResetToken() {
        byte[] bytes = new byte[RESET_TOKEN_BYTES];
        random.nextBytes(bytes);
        return urlEncoder.encodeToString(bytes);
    }
}
