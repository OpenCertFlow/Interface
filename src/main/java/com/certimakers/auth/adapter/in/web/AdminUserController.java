package com.certimakers.auth.adapter.in.web;

import com.certimakers.auth.application.port.in.ManageUserRoleUseCase;
import com.certimakers.auth.application.port.in.QueryUsersUseCase;
import com.certimakers.auth.application.port.in.QueryUsersUseCase.UserSummary;
import com.certimakers.common.adapter.in.web.annotation.WebAdapter;
import com.certimakers.common.adapter.in.web.response.ApiResponse;
import com.certimakers.common.adapter.in.web.trace.TraceId;
import com.certimakers.common.domain.port.TimeProvider;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import reactor.core.publisher.Mono;

/**
 * 관리자 전용 사용자 관리 API.
 *
 * <p>접근 제어는 시큐리티 경로 규칙({@code /api/v1/admin/**} → ADMIN)이 담당한다. 컨트롤러에서 다시
 * 권한을 확인하지 않는 이유는 판단이 두 곳에 있으면 언젠가 어긋나기 때문이다.
 */
@WebAdapter
@RequestMapping("/api/v1/admin/users")
public class AdminUserController {

    private final ManageUserRoleUseCase manageUserRoleUseCase;
    private final QueryUsersUseCase queryUsersUseCase;
    private final TimeProvider timeProvider;

    public AdminUserController(
            ManageUserRoleUseCase manageUserRoleUseCase, QueryUsersUseCase queryUsersUseCase,
            TimeProvider timeProvider) {
        this.manageUserRoleUseCase = manageUserRoleUseCase;
        this.queryUsersUseCase = queryUsersUseCase;
        this.timeProvider = timeProvider;
    }

    @GetMapping
    public Mono<ResponseEntity<ApiResponse<List<UserSummary>>>> list(
            @RequestParam(required = false) String role,
            @RequestParam(required = false, defaultValue = "50") int limit) {

        return queryUsersUseCase.list(role, limit)
                .flatMap(body -> TraceId.current().map(traceId -> ResponseEntity
                        .status(HttpStatus.OK)
                        .body(ApiResponse.success(body, traceId, timeProvider.now()))));
    }

    @PatchMapping("/{userId}/role")
    public Mono<ResponseEntity<ApiResponse<AuthResponses.Profile>>> changeRole(
            @PathVariable String userId,
            @Valid @RequestBody ChangeRoleRequest request) {

        return manageUserRoleUseCase.changeRole(
                        new ManageUserRoleUseCase.ChangeRoleCommand(userId, request.role()))
                .map(AuthResponses.Profile::from)
                .flatMap(body -> TraceId.current().map(traceId -> ResponseEntity
                        .status(HttpStatus.OK)
                        .body(ApiResponse.success(body, traceId, timeProvider.now()))));
    }

    /** @param role USER·CONSULTANT·ADMIN 중 하나. 컨설턴트 승인은 CONSULTANT로 승격하는 것이다(F-WADM-003) */
    public record ChangeRoleRequest(
            @NotBlank(message = "권한 값이 필요합니다.")
            String role) {
    }
}
