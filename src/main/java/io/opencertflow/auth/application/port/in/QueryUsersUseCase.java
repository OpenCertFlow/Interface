package io.opencertflow.auth.application.port.in;

import java.time.Instant;
import java.util.List;
import reactor.core.publisher.Mono;

/** 관리자 사용자 목록 조회(F-WADM-002). 역할 필터·상한을 받는다. */
public interface QueryUsersUseCase {

    Mono<List<UserSummary>> list(String roleFilter, int limit);

    record UserSummary(String id, String email, String nickname, String role, String provider,
                       boolean emailVerified, Instant createdAt) {
    }
}
