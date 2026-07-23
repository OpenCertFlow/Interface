package com.certimakers.auth.application.service;

import com.certimakers.auth.application.port.in.ManageUserRoleUseCase;
import com.certimakers.auth.application.port.out.LoadUserPort;
import com.certimakers.auth.application.port.out.SaveUserPort;
import com.certimakers.auth.domain.error.AuthErrorCode;
import com.certimakers.auth.domain.model.Role;
import com.certimakers.auth.domain.model.User;
import com.certimakers.auth.domain.model.UserId;
import com.certimakers.common.application.annotation.UseCase;
import com.certimakers.common.application.support.BlockingBridge;
import com.certimakers.common.domain.error.BusinessException;
import java.util.Arrays;
import java.util.Locale;
import java.util.UUID;
import reactor.core.publisher.Mono;

/** 사용자 권한 변경 오케스트레이션. 조회·저장이 JPA라 BlockingBridge로 감싼다. */
@UseCase
public class UserRoleService implements ManageUserRoleUseCase {

    private final LoadUserPort loadUserPort;
    private final SaveUserPort saveUserPort;
    private final BlockingBridge blockingBridge;

    public UserRoleService(
            LoadUserPort loadUserPort, SaveUserPort saveUserPort, BlockingBridge blockingBridge) {
        this.loadUserPort = loadUserPort;
        this.saveUserPort = saveUserPort;
        this.blockingBridge = blockingBridge;
    }

    @Override
    public Mono<User> changeRole(ChangeRoleCommand command) {
        Role role = parseRole(command.role());
        UserId targetId = parseId(command.targetUserId());

        return blockingBridge.mono(() -> {
            User user = loadUserPort.findById(targetId)
                    .orElseThrow(() -> new BusinessException(AuthErrorCode.USER_NOT_FOUND));
            user.changeRole(role);
            return saveUserPort.save(user);
        });
    }

    private Role parseRole(String raw) {
        if (raw == null || raw.isBlank()) {
            throw BusinessException.invalid("권한 값이 필요합니다.");
        }
        try {
            return Role.valueOf(raw.strip().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw BusinessException.invalid("권한 값이 올바르지 않습니다. 가능한 값: %s".formatted(
                    Arrays.stream(Role.values()).map(Enum::name)
                            .collect(java.util.stream.Collectors.joining(", "))));
        }
    }

    private UserId parseId(String raw) {
        try {
            return UserId.of(UUID.fromString(raw));
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BusinessException(AuthErrorCode.USER_NOT_FOUND);
        }
    }
}
