package io.opencertflow.auth.config;

import io.opencertflow.common.config.SecurityProperties;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.util.StringUtils;

/**
 * 운영 프로파일에서 안전하지 않은 기본값으로 기동하는 것을 막는다.
 *
 * <p>이 프로젝트는 오픈소스다. {@code application.yml}의 개발용 JWT 기본 시크릿은 저장소를 읽은
 * 누구나 알고 있으며, 그 값으로 기동한 서버는 <b>임의의 관리자 토큰을 위조당한다.</b> 암호화 키가
 * 없으면 임시 키가 생성되어 재기동 시 기존 개인정보를 복호화할 수 없게 된다.
 *
 * <p>두 경우 모두 로그 경고만으로는 부족하다 — 경고는 읽히지 않고, 증상은 사고가 난 뒤에 나타난다.
 * {@code prod} 프로파일에서는 <b>기동을 실패시킨다.</b>
 *
 * <p>로컬·테스트·시연({@code local}, 기본 프로파일)에는 적용되지 않는다. 개발자가 환경변수 없이
 * 바로 띄울 수 있어야 하기 때문이다.
 *
 * <p><b>왜 {@code auth} 안에 있는가.</b> 이 클래스는 auth의 JWT 시크릿과 common의 암호화 키를
 * 함께 본다. {@code common.config}에 두면 common이 auth를 참조하게 되는데, auth는 이미 common에
 * 의존하므로 두 컨텍스트 사이에 순환이 생긴다(ArchUnit이 실제로 잡았다). auth → common은 허용된
 * 방향이므로 이쪽에 둔다.
 */
@Configuration
@Profile("prod")
public class ProductionSecretGuard implements InitializingBean {

    /** application.yml에 적힌 개발용 기본값. 이 값이 운영에 올라오면 안 된다. */
    private static final String DEV_JWT_SECRET =
            "opencertflow-local-development-secret-key-do-not-use-in-production";

    /** HS256의 최소 키 길이. 짧으면 jjwt가 거부하지만, 그 실패는 첫 로그인 시점에 드러난다. */
    private static final int MIN_JWT_SECRET_BYTES = 32;

    private final AuthProperties authProperties;
    private final SecurityProperties securityProperties;

    public ProductionSecretGuard(
            AuthProperties authProperties, SecurityProperties securityProperties) {
        this.authProperties = authProperties;
        this.securityProperties = securityProperties;
    }

    @Override
    public void afterPropertiesSet() {
        List<String> problems = new ArrayList<>();

        String jwtSecret = authProperties.jwt().secret();
        if (!StringUtils.hasText(jwtSecret) || DEV_JWT_SECRET.equals(jwtSecret)) {
            problems.add("OPENCERTFLOW_AUTH_JWT_SECRET이 개발용 기본값입니다. "
                    + "이 값은 공개 저장소에 있어 누구나 관리자 토큰을 위조할 수 있습니다.");
        } else if (jwtSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8).length
                < MIN_JWT_SECRET_BYTES) {
            problems.add("OPENCERTFLOW_AUTH_JWT_SECRET이 너무 짧습니다(HS256은 32바이트 이상 필요).");
        }

        if (!StringUtils.hasText(securityProperties.encryptionKey())) {
            problems.add("OPENCERTFLOW_SECURITY_ENCRYPTION_KEY가 없습니다. "
                    + "임시 키로 기동하면 재시작 시 기존 개인정보를 복호화할 수 없습니다.");
        }

        if (!problems.isEmpty()) {
            throw new IllegalStateException(
                    "운영 프로파일 기동을 중단합니다 — 필수 시크릿이 설정되지 않았습니다:\n"
                            + String.join("\n", problems.stream().map(p -> "  - " + p).toList())
                            + "\n자세한 내용은 SECURITY.md의 '운영 배포 시 필수 설정'을 참고하세요.");
        }
    }
}
