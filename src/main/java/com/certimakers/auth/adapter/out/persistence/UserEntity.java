package com.certimakers.auth.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * {@code app_user} 테이블 매핑. 테이블명은 {@code user}가 PostgreSQL 예약어라 {@code app_user}로 둔다.
 *
 * <p>{@code passwordHash}는 소셜 계정이면 null이다. {@code providerId}는 카카오 회원번호를 담으며
 * 로컬 계정이면 null이다.
 */
@Entity
@Table(name = "app_user")
public class UserEntity {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash")
    private String passwordHash; // 소셜 계정이면 null

    @Column(nullable = false)
    private String nickname;

    @Column(nullable = false)
    private String role;

    @Column(nullable = false)
    private String provider;

    @Column(name = "provider_id")
    private String providerId; // 로컬 계정이면 null

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected UserEntity() {
    }

    public UserEntity(
            UUID id, String email, String passwordHash, String nickname, String role,
            String provider, String providerId, boolean emailVerified, Instant createdAt) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.nickname = nickname;
        this.role = role;
        this.provider = provider;
        this.providerId = providerId;
        this.emailVerified = emailVerified;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getNickname() {
        return nickname;
    }

    public String getRole() {
        return role;
    }

    public String getProvider() {
        return provider;
    }

    public String getProviderId() {
        return providerId;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
