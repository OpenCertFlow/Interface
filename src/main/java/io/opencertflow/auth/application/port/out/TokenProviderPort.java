package io.opencertflow.auth.application.port.out;

import io.opencertflow.auth.domain.model.User;
import java.util.Optional;

/**
 * 액세스·리프레시 토큰 발급과 액세스 토큰 검증. 구현은 JWT다.
 *
 * <p>서명·검증은 순수 CPU 연산(IO 없음)이라 이벤트 루프에서 호출해도 블로킹이 아니다. 따라서
 * 시큐리티 필터가 요청마다 {@link #parseAccessToken}을 직접 호출해도 된다.
 */
public interface TokenProviderPort {

    /** 로그인 성공 시 액세스·리프레시 토큰 쌍을 발급한다. */
    IssuedTokens issue(User user);

    /**
     * 액세스 토큰을 검증하고 클레임을 돌려준다. 서명 불일치·만료면 비어 있음을 반환한다 —
     * 예외가 아니라 Optional로 다뤄 필터가 조용히 익명 처리하도록 한다.
     */
    Optional<AuthenticatedUser> parseAccessToken(String accessToken);

    /** 발급된 토큰 쌍. 값은 이미 서명된 JWT 문자열이다. */
    record IssuedTokens(String accessToken, String refreshToken, long accessTokenExpiresInSeconds) {
    }

    /** 액세스 토큰에서 복원한 인증 주체. 시큐리티 컨텍스트에 실린다. */
    record AuthenticatedUser(String userId, String role) {
    }
}
