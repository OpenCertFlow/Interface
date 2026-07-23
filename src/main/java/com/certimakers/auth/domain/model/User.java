package com.certimakers.auth.domain.model;

import com.certimakers.auth.domain.error.AuthErrorCode;
import com.certimakers.common.domain.error.BusinessException;
import com.certimakers.common.domain.model.AggregateRoot;
import com.certimakers.common.domain.model.Guard;
import java.time.Instant;
import java.util.Optional;

/**
 * 사용자 애그리거트 루트. 계정 하나의 상태 전체를 담으며, 한 트랜잭션에서 통째로 저장·조회된다.
 *
 * <p>비밀번호 <b>해시</b>만 보유하고 평문은 절대 담지 않는다. 해싱·검증은 {@code PasswordEncoderPort}가
 * 어댑터에서 수행하며, 도메인은 "이 해시가 이 계정의 것"이라는 사실만 안다.
 *
 * <p>소셜 계정(카카오)은 비밀번호가 없다. 그런 계정에 비밀번호 관련 조작을 시도하면
 * {@link AuthErrorCode#SOCIAL_ACCOUNT_NO_PASSWORD}로 거부한다 — 상태의 일관성을 애그리거트가 지킨다.
 */
public class User extends AggregateRoot<UserId> {

    private final UserId id;
    private final Email email;
    private final AuthProvider provider;
    private final String providerId; // 소셜 계정의 외부 식별자. LOCAL이면 null
    private final Instant createdAt;

    private PasswordHash passwordHash; // 소셜 계정이면 null
    private Nickname nickname;
    private Role role;
    private boolean emailVerified;

    private User(
            UserId id, Email email, PasswordHash passwordHash, Nickname nickname, Role role,
            AuthProvider provider, String providerId, boolean emailVerified, Instant createdAt) {
        this.id = Guard.notNull(id, "id");
        this.email = Guard.notNull(email, "email");
        this.nickname = Guard.notNull(nickname, "nickname");
        this.role = Guard.notNull(role, "role");
        this.provider = Guard.notNull(provider, "provider");
        this.providerId = providerId;
        this.passwordHash = passwordHash;
        this.emailVerified = emailVerified;
        this.createdAt = Guard.notNull(createdAt, "createdAt");
    }

    /** 이메일·비밀번호로 새 로컬 계정을 만든다. 이메일 인증 전 상태로 시작한다. */
    public static User registerLocal(
            UserId id, Email email, PasswordHash passwordHash, Nickname nickname, Instant createdAt) {
        Guard.notNull(passwordHash, "passwordHash");
        return new User(
                id, email, passwordHash, nickname, Role.USER,
                AuthProvider.LOCAL, null, false, createdAt);
    }

    /**
     * 카카오 프로필로 새 소셜 계정을 만든다. 카카오가 이미 이메일을 검증했으므로 인증 완료 상태로
     * 시작한다.
     */
    public static User registerKakao(
            UserId id, Email email, Nickname nickname, String kakaoId, Instant createdAt) {
        Guard.hasText(kakaoId, "kakaoId");
        return new User(
                id, email, null, nickname, Role.USER,
                AuthProvider.KAKAO, kakaoId, true, createdAt);
    }

    /** 저장된 상태에서 되살린다(영속성 재구성 전용). 전이 규칙을 거치지 않는다. */
    public static User reconstitute(
            UserId id, Email email, PasswordHash passwordHash, Nickname nickname, Role role,
            AuthProvider provider, String providerId, boolean emailVerified, Instant createdAt) {
        return new User(
                id, email, passwordHash, nickname, role, provider, providerId, emailVerified, createdAt);
    }

    /** 이메일 인증을 완료한다. 이미 완료된 상태여도 문제없다(멱등). */
    public void verifyEmail() {
        this.emailVerified = true;
    }

    /** 마이페이지에서 닉네임을 바꾼다. */
    public void changeNickname(Nickname newNickname) {
        this.nickname = Guard.notNull(newNickname, "nickname");
    }

    /**
     * 비밀번호를 새 해시로 교체한다. 소셜 계정에는 허용하지 않는다 — 카카오 계정에 비밀번호를 심으면
     * 로그인 경로가 두 갈래가 되어 계정 탈취 표면이 넓어진다.
     */
    public void changePassword(PasswordHash newHash) {
        requireLocalAccount();
        this.passwordHash = Guard.notNull(newHash, "passwordHash");
    }

    /**
     * 비밀번호 검증에 쓸 해시를 노출한다. 소셜 계정이면 비밀번호 자체가 없으므로 거부한다 —
     * 호출자는 이 예외로 "소셜 계정입니다" 안내를 띄운다.
     */
    public PasswordHash passwordHashForVerification() {
        requireLocalAccount();
        return passwordHash;
    }

    private void requireLocalAccount() {
        if (provider.isSocial()) {
            throw new BusinessException(AuthErrorCode.SOCIAL_ACCOUNT_NO_PASSWORD);
        }
    }

    @Override
    public UserId id() {
        return id;
    }

    public Email email() {
        return email;
    }

    public Optional<PasswordHash> passwordHash() {
        return Optional.ofNullable(passwordHash);
    }

    public Nickname nickname() {
        return nickname;
    }

    public Role role() {
        return role;
    }

    public AuthProvider provider() {
        return provider;
    }

    public Optional<String> providerId() {
        return Optional.ofNullable(providerId);
    }

    public boolean emailVerified() {
        return emailVerified;
    }

    public Instant createdAt() {
        return createdAt;
    }
}
