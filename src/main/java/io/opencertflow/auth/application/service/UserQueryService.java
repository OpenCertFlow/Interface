package io.opencertflow.auth.application.service;

import io.opencertflow.auth.application.port.in.QueryUsersUseCase;
import io.opencertflow.auth.application.port.out.UserAdminQueryPort;
import io.opencertflow.auth.domain.model.Role;
import io.opencertflow.common.application.annotation.UseCase;
import io.opencertflow.common.application.support.BlockingBridge;
import io.opencertflow.common.domain.error.BusinessException;
import java.util.List;
import java.util.Locale;
import reactor.core.publisher.Mono;

/** 관리자 사용자 목록 조회. 역할 필터가 있으면 유효한 역할인지 검증한다. */
@UseCase
public class UserQueryService implements QueryUsersUseCase {

    private static final int MAX_LIMIT = 200;
    private static final int DEFAULT_LIMIT = 50;

    private final UserAdminQueryPort userQueryPort;
    private final BlockingBridge blockingBridge;

    public UserQueryService(UserAdminQueryPort userQueryPort, BlockingBridge blockingBridge) {
        this.userQueryPort = userQueryPort;
        this.blockingBridge = blockingBridge;
    }

    @Override
    public Mono<List<UserSummary>> list(String roleFilter, int limit) {
        return Mono.fromSupplier(() -> normalizeRole(roleFilter))
                .flatMap(role -> {
                    int capped = limit <= 0 ? DEFAULT_LIMIT : Math.min(limit, MAX_LIMIT);
                    return blockingBridge.mono(() -> userQueryPort.findUsers(role, capped).stream()
                            .map(user -> new UserSummary(
                                    user.id().value().toString(), user.email().value(),
                                    user.nickname().value(), user.role().name(),
                                    user.provider().name(), user.emailVerified(), user.createdAt()))
                            .toList());
                });
    }

    private String normalizeRole(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Role.valueOf(raw.strip().toUpperCase(Locale.ROOT)).name();
        } catch (IllegalArgumentException e) {
            throw BusinessException.invalid("역할 값이 올바르지 않습니다: " + raw);
        }
    }
}
