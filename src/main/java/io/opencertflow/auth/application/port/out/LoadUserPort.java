package io.opencertflow.auth.application.port.out;

import io.opencertflow.auth.domain.model.Email;
import io.opencertflow.auth.domain.model.User;
import io.opencertflow.auth.domain.model.UserId;
import java.util.Optional;

/** 사용자 조회. 블로킹이므로 호출자는 BlockingBridge로 감싼다. */
public interface LoadUserPort {

    Optional<User> findById(UserId id);

    Optional<User> findByEmail(Email email);

    /** 카카오 계정을 provider 식별자로 찾는다. 이미 연동된 계정을 다시 만들지 않기 위함. */
    Optional<User> findByKakaoId(String kakaoId);

    /** 구글 계정을 provider 식별자로 찾는다. 이미 연동된 계정을 다시 만들지 않기 위함. */
    Optional<User> findByGoogleId(String googleId);

    boolean existsByEmail(Email email);
}
