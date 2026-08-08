package io.opencertflow.auth.application.service;

import io.opencertflow.auth.application.port.in.ManageUserRoleUseCase;
import io.opencertflow.auth.application.port.out.LoadUserPort;
import io.opencertflow.auth.application.port.out.SaveUserPort;
import io.opencertflow.auth.domain.error.AuthErrorCode;
import io.opencertflow.auth.domain.model.Role;
import io.opencertflow.auth.domain.model.User;
import io.opencertflow.auth.domain.model.UserId;
import io.opencertflow.common.application.annotation.UseCase;
import io.opencertflow.common.application.support.BlockingBridge;
import io.opencertflow.common.domain.error.BusinessException;
import java.util.Arrays;
import java.util.Locale;
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
            return UserId.of(Long.parseLong(raw));
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BusinessException(AuthErrorCode.USER_NOT_FOUND);
        }
    }
}
