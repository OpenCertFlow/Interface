package com.certimakers.auth.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.certimakers.auth.application.port.in.GoogleLoginUseCase.GoogleLoginCommand;
import com.certimakers.auth.application.port.out.GoogleClientPort;
import com.certimakers.auth.application.port.out.GoogleClientPort.GoogleProfile;
import com.certimakers.auth.application.port.out.LoadUserPort;
import com.certimakers.auth.application.port.out.RefreshTokenStorePort;
import com.certimakers.auth.application.port.out.SaveUserPort;
import com.certimakers.auth.application.port.out.TokenProviderPort;
import com.certimakers.auth.application.port.out.TokenProviderPort.IssuedTokens;
import com.certimakers.auth.domain.model.AuthProvider;
import com.certimakers.auth.domain.model.Email;
import com.certimakers.auth.domain.model.Nickname;
import com.certimakers.auth.domain.model.User;
import com.certimakers.auth.domain.model.UserId;
import com.certimakers.common.application.support.BlockingBridge;
import com.certimakers.common.domain.port.IdGenerator;
import com.certimakers.common.domain.port.TimeProvider;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

/**
 * 구글 로그인 오케스트레이션 검증. 외부 구글 서버와 저장소는 목으로 대체하고, "코드 → 프로필 →
 * 계정 매칭/생성 → 토큰 발급" 흐름만 확인한다.
 *
 * <p>핵심은 <b>이미 연동된 계정은 다시 만들지 않고</b>(findByGoogleId 히트 시 save 미호출),
 * 처음 온 계정은 즉시 가입시킨다는 것이다.
 */
class GoogleAuthServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-10T12:00:00Z");
    private static final GoogleProfile PROFILE =
            new GoogleProfile("google-sub-1", "user@gmail.com", "홍길동");

    private GoogleClientPort googleClient;
    private LoadUserPort loadUserPort;
    private SaveUserPort saveUserPort;
    private TokenProviderPort tokenProvider;
    private RefreshTokenStorePort refreshTokenStore;
    private GoogleAuthService service;

    @BeforeEach
    void setUp() {
        googleClient = Mockito.mock(GoogleClientPort.class);
        loadUserPort = Mockito.mock(LoadUserPort.class);
        saveUserPort = Mockito.mock(SaveUserPort.class);
        tokenProvider = Mockito.mock(TokenProviderPort.class);
        refreshTokenStore = Mockito.mock(RefreshTokenStorePort.class);
        IdGenerator idGenerator = com.certimakers.support.TestIds::next;
        TimeProvider timeProvider = new TimeProvider() {
            @Override
            public Instant now() {
                return NOW;
            }

            @Override
            public java.time.ZoneId zone() {
                return java.time.ZoneId.of("UTC");
            }
        };

        when(googleClient.fetchProfile(anyString())).thenReturn(Mono.just(PROFILE));
        when(tokenProvider.issue(any()))
                .thenReturn(new IssuedTokens("access-jwt", "refresh-jwt", 1800));
        when(refreshTokenStore.save(anyString(), anyString())).thenReturn(Mono.empty());

        service = new GoogleAuthService(
                googleClient, loadUserPort, saveUserPort, tokenProvider, refreshTokenStore,
                new BlockingBridge(Schedulers.immediate()), idGenerator, timeProvider);
    }

    @Test
    @DisplayName("처음 온 구글 계정은 즉시 가입시키고 토큰을 발급한다")
    void 신규_계정은_가입시킨다() {
        when(loadUserPort.findByGoogleId("google-sub-1")).thenReturn(Optional.empty());
        when(saveUserPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        StepVerifier.create(service.login(new GoogleLoginCommand("auth-code")))
                .assertNext(tokens -> assertThat(tokens.accessToken()).isEqualTo("access-jwt"))
                .verifyComplete();

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(saveUserPort).save(saved.capture());
        assertThat(saved.getValue().provider()).isEqualTo(AuthProvider.GOOGLE);
        assertThat(saved.getValue().providerId()).contains("google-sub-1");
        assertThat(saved.getValue().email().value()).isEqualTo("user@gmail.com");
        verify(refreshTokenStore).save(eq(saved.getValue().id().value().toString()), eq("refresh-jwt"));
    }

    @Test
    @DisplayName("이미 연동된 구글 계정은 다시 만들지 않고 로그인만 시킨다")
    void 기존_계정은_다시_만들지_않는다() {
        User existing = User.registerGoogle(
                UserId.of(com.certimakers.support.TestIds.next()), Email.of("user@gmail.com"),
                Nickname.of("홍길동"), "google-sub-1", NOW);
        when(loadUserPort.findByGoogleId("google-sub-1")).thenReturn(Optional.of(existing));

        StepVerifier.create(service.login(new GoogleLoginCommand("auth-code")))
                .assertNext(tokens -> assertThat(tokens.refreshToken()).isEqualTo("refresh-jwt"))
                .verifyComplete();

        verify(saveUserPort, never()).save(any());
    }
}
