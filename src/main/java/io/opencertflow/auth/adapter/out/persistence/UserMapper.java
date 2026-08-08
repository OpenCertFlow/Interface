package io.opencertflow.auth.adapter.out.persistence;

import io.opencertflow.auth.domain.model.AuthProvider;
import io.opencertflow.auth.domain.model.Email;
import io.opencertflow.auth.domain.model.Nickname;
import io.opencertflow.auth.domain.model.PasswordHash;
import io.opencertflow.auth.domain.model.Role;
import io.opencertflow.auth.domain.model.User;
import io.opencertflow.auth.domain.model.UserId;

/** 사용자 도메인 ↔ 엔티티 매핑. */
public final class UserMapper {

    private UserMapper() {
    }

    public static UserEntity toEntity(User user) {
        return new UserEntity(
                user.id().value(),
                user.email().value(),
                user.passwordHash().map(PasswordHash::value).orElse(null),
                user.nickname().value(),
                user.role().name(),
                user.provider().name(),
                user.providerId().orElse(null),
                user.emailVerified(),
                user.createdAt());
    }

    public static User toDomain(UserEntity entity) {
        PasswordHash hash = entity.getPasswordHash() != null
                ? PasswordHash.of(entity.getPasswordHash())
                : null;
        return User.reconstitute(
                UserId.of(entity.getId()),
                Email.of(entity.getEmail()),
                hash,
                Nickname.of(entity.getNickname()),
                Role.valueOf(entity.getRole()),
                AuthProvider.valueOf(entity.getProvider()),
                entity.getProviderId(),
                entity.isEmailVerified(),
                entity.getCreatedAt());
    }
}
