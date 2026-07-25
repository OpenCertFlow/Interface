package com.certimakers.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.certimakers.auth.application.port.out.LoadUserPort;
import com.certimakers.auth.application.port.out.SaveUserPort;
import com.certimakers.auth.domain.model.AuthProvider;
import com.certimakers.auth.domain.model.Email;
import com.certimakers.auth.domain.model.Nickname;
import com.certimakers.auth.domain.model.User;
import com.certimakers.auth.domain.model.UserId;
import com.certimakers.common.domain.port.IdGenerator;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * 소셜 로그인 provider가 실제 DB 제약을 통과하는지 검증한다.
 *
 * <p>단위 테스트(GoogleAuthServiceTest)는 SaveUserPort를 목으로 대체해 DB의 {@code ck_app_user_provider}
 * CHECK 제약을 타지 않는다. 그래서 코드가 GOOGLE을 써도 제약이 KAKAO까지만 허용하면 실제 가입에서만
 * 500이 터진다. 이 테스트는 실제 저장까지 내려가 그 격차를 잡는다.
 */
@SpringBootTest
@ActiveProfiles("local")
@Testcontainers
class SocialProviderPersistenceTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    SaveUserPort saveUserPort;

    @Autowired
    LoadUserPort loadUserPort;

    @Autowired
    IdGenerator idGenerator;

    @Test
    @DisplayName("구글·카카오 provider 계정을 저장·조회할 수 있다 — provider CHECK가 세 값을 허용한다")
    void 소셜_provider_계정을_저장한다() {
        Email googleEmail = Email.of("gtester@gmail.com");
        saveUserPort.save(User.registerGoogle(
                UserId.of(idGenerator.nextId()), googleEmail, Nickname.of("구글유저"),
                "google-123", Instant.now()));

        Email kakaoEmail = Email.of("ktester@gmail.com");
        saveUserPort.save(User.registerKakao(
                UserId.of(idGenerator.nextId()), kakaoEmail, Nickname.of("카카오유저"),
                "kakao-456", Instant.now()));

        Optional<User> google = loadUserPort.findByEmail(googleEmail);
        assertThat(google).isPresent();
        assertThat(google.get().provider()).isEqualTo(AuthProvider.GOOGLE);

        Optional<User> kakao = loadUserPort.findByEmail(kakaoEmail);
        assertThat(kakao).isPresent();
        assertThat(kakao.get().provider()).isEqualTo(AuthProvider.KAKAO);
    }
}
