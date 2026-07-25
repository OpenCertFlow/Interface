package com.certimakers.auth.application.port.in;

import com.certimakers.auth.domain.model.UserId;
import java.util.List;
import reactor.core.publisher.Mono;

/** 이메일·비밀번호 회원가입. */
public interface SignUpUseCase {

    Mono<UserId> signUp(SignUpCommand command);

    /**
     * @param email          가입 이메일
     * @param rawPassword    평문 비밀번호. 서비스가 즉시 해싱하며 도메인·저장소에 평문이 흐르지 않는다
     * @param nickname       표시 이름
     * @param agreedTermKeys 동의한 약관 키 목록. 필수 약관이 모두 포함되어야 가입된다(F-AUTH-008)
     */
    record SignUpCommand(
            String email, String rawPassword, String nickname, List<String> agreedTermKeys) {

        public SignUpCommand {
            agreedTermKeys = agreedTermKeys == null ? List.of() : List.copyOf(agreedTermKeys);
        }
    }
}
