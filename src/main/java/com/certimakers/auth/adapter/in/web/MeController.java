package com.certimakers.auth.adapter.in.web;

import com.certimakers.auth.application.port.in.LogoutUseCase;
import com.certimakers.auth.application.port.in.MyProfileUseCase;
import com.certimakers.auth.application.port.in.WithdrawAccountUseCase;
import com.certimakers.auth.domain.error.AuthErrorCode;
import com.certimakers.common.adapter.in.web.annotation.WebAdapter;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.certimakers.common.adapter.in.web.response.ApiResponse;
import com.certimakers.common.adapter.in.web.trace.TraceId;
import com.certimakers.common.domain.error.BusinessException;
import com.certimakers.common.domain.port.TimeProvider;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import reactor.core.publisher.Mono;

/**
 * 마이페이지 API. <b>인증이 필요한</b> 엔드포인트 모음이다.
 *
 * <p>사용자 식별자는 요청 본문이 아니라 인증 주체({@link Principal})에서 가져온다. 본문으로 받으면
 * 남의 userId를 넣어 다른 사람의 정보를 조회·수정할 수 있다.
 */
@Tag(name = "마이페이지", description = "내 정보·비밀번호·로그아웃·계정 탈퇴")
@WebAdapter
@RequestMapping("/api/v1/me")
public class MeController {

    private final MyProfileUseCase myProfileUseCase;
    private final LogoutUseCase logoutUseCase;
    private final WithdrawAccountUseCase withdrawAccountUseCase;
    private final TimeProvider timeProvider;

    public MeController(
            MyProfileUseCase myProfileUseCase,
            LogoutUseCase logoutUseCase,
            WithdrawAccountUseCase withdrawAccountUseCase,
            TimeProvider timeProvider) {
        this.myProfileUseCase = myProfileUseCase;
        this.logoutUseCase = logoutUseCase;
        this.withdrawAccountUseCase = withdrawAccountUseCase;
        this.timeProvider = timeProvider;
    }

    @GetMapping
    public Mono<ResponseEntity<ApiResponse<AuthResponses.Profile>>> getProfile(
            Mono<Principal> principal) {

        return currentUserId(principal)
                .flatMap(myProfileUseCase::getProfile)
                .map(AuthResponses.Profile::from)
                .flatMap(body -> wrap(body, HttpStatus.OK));
    }

    @PatchMapping("/nickname")
    public Mono<ResponseEntity<ApiResponse<AuthResponses.Profile>>> updateNickname(
            Mono<Principal> principal,
            @Valid @RequestBody AuthRequests.UpdateNickname request) {

        return currentUserId(principal)
                .flatMap(userId -> myProfileUseCase.updateNickname(
                        new MyProfileUseCase.UpdateNicknameCommand(userId, request.nickname())))
                .map(AuthResponses.Profile::from)
                .flatMap(body -> wrap(body, HttpStatus.OK));
    }

    @PatchMapping("/password")
    public Mono<ResponseEntity<ApiResponse<Void>>> changePassword(
            Mono<Principal> principal,
            @Valid @RequestBody AuthRequests.ChangePassword request) {

        return currentUserId(principal)
                .flatMap(userId -> myProfileUseCase.changePassword(
                        new MyProfileUseCase.ChangePasswordCommand(
                                userId, request.currentPassword(), request.newPassword())))
                .then(wrap(null, HttpStatus.OK));
    }

    /**
     * 로그아웃(F-AUTH-013). 요청 본문의 리프레시 토큰이 가리키는 <b>현재 세션만</b> 폐기하고, 같은
     * 계정의 다른 기기 세션은 유지한다. 액세스 토큰은 만료까지 유효하다(짧게 설정).
     */
    @DeleteMapping("/session")
    public Mono<ResponseEntity<ApiResponse<Void>>> logout(
            Mono<Principal> principal, @Valid @RequestBody AuthRequests.Logout request) {
        return currentUserId(principal)
                .flatMap(userId -> logoutUseCase.logout(userId, request.refreshToken()))
                .then(wrap(null, HttpStatus.NO_CONTENT));
    }

    /** 계정 탈퇴(F-AUTH-018). 계정을 삭제하고 세션을 폐기한다. 인증 주체 본인만 자신을 지운다. */
    @DeleteMapping
    public Mono<ResponseEntity<ApiResponse<Void>>> withdraw(Mono<Principal> principal) {
        return currentUserId(principal)
                .flatMap(withdrawAccountUseCase::withdraw)
                .then(wrap(null, HttpStatus.NO_CONTENT));
    }

    /** 인증 주체의 이름이 곧 userId다(JWT의 subject). 없으면 401로 이어진다. */
    private Mono<String> currentUserId(Mono<Principal> principal) {
        return principal.map(Principal::getName)
                .switchIfEmpty(Mono.error(new BusinessException(AuthErrorCode.UNAUTHENTICATED)));
    }

    private <T> Mono<ResponseEntity<ApiResponse<T>>> wrap(T body, HttpStatus status) {
        return TraceId.current().map(traceId -> ResponseEntity.status(status)
                .body(ApiResponse.success(body, traceId, timeProvider.now())));
    }
}
