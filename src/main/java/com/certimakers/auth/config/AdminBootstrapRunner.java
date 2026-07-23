package com.certimakers.auth.config;

import com.certimakers.auth.application.port.out.LoadUserPort;
import com.certimakers.auth.application.port.out.SaveUserPort;
import com.certimakers.auth.domain.model.Email;
import com.certimakers.auth.domain.model.Role;
import com.certimakers.auth.domain.model.User;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 설정에 지정된 이메일의 계정을 시작 시점에 관리자로 승격한다.
 *
 * <p><b>최초 관리자 문제를 푸는 장치다.</b> 공지·자료실 작성과 권한 부여 API가 모두 관리자를 요구하는데,
 * 관리자를 만들 방법이 그 API뿐이면 아무도 관리자가 될 수 없다. 설정으로 첫 관리자를 지정하고,
 * 그 이후는 관리자 API로 위임한다.
 *
 * <p>가입되지 않은 이메일은 조용히 넘어간다 — 관리자가 될 사람이 아직 가입하지 않았을 뿐이며,
 * 가입 후 재시작하면 승격된다. 이 때문에 애플리케이션이 뜨지 못하게 만들 이유는 없다.
 *
 * <p>승격은 <b>멱등</b>하다. 이미 관리자면 저장하지 않는다.
 */
@Component
public class AdminBootstrapRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapRunner.class);

    private final LoadUserPort loadUserPort;
    private final SaveUserPort saveUserPort;
    private final List<String> adminEmails;

    public AdminBootstrapRunner(
            LoadUserPort loadUserPort, SaveUserPort saveUserPort, AuthProperties properties) {
        this.loadUserPort = loadUserPort;
        this.saveUserPort = saveUserPort;
        this.adminEmails = properties.bootstrapAdminEmails() == null
                ? List.of()
                : properties.bootstrapAdminEmails();
    }

    /**
     * 시작 스레드에서 실행된다. 블로킹 JPA 호출이지만 이벤트 루프가 아니므로 문제가 되지 않는다.
     */
    @Override
    public void run(ApplicationArguments args) {
        for (String rawEmail : adminEmails) {
            if (rawEmail == null || rawEmail.isBlank()) {
                continue;
            }
            promote(rawEmail.strip());
        }
    }

    private void promote(String rawEmail) {
        try {
            Email email = Email.of(rawEmail);
            User user = loadUserPort.findByEmail(email).orElse(null);

            if (user == null) {
                log.info("관리자 지정 대상이 아직 가입하지 않았습니다. 가입 후 재시작하면 승격됩니다. email={}", rawEmail);
                return;
            }
            if (user.isAdmin()) {
                return;
            }
            user.changeRole(Role.ADMIN);
            saveUserPort.save(user);
            log.info("관리자로 승격했습니다. email={}", rawEmail);

        } catch (RuntimeException e) {
            // 부트스트랩 실패가 서비스 기동을 막아서는 안 된다. 진단 기능은 관리자 없이도 동작한다.
            log.warn("관리자 승격에 실패했습니다. email={}, cause={}", rawEmail, e.toString());
        }
    }
}
