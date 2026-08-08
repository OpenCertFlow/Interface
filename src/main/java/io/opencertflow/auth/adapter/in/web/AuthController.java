package io.opencertflow.auth.adapter.in.web;

import io.opencertflow.auth.application.port.in.EmailVerificationUseCase;
import io.opencertflow.auth.application.port.in.GoogleLoginUseCase;
import io.opencertflow.auth.application.port.in.KakaoLoginUseCase;
import io.opencertflow.auth.application.port.in.LoginUseCase;
import io.opencertflow.auth.application.port.in.PasswordResetUseCase;
import io.opencertflow.auth.application.port.in.RefreshTokenUseCase;
import io.opencertflow.auth.application.port.in.SignUpUseCase;
import io.opencertflow.common.adapter.in.web.annotation.WebAdapter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.opencertflow.common.adapter.in.web.response.ApiResponse;
import io.opencertflow.common.adapter.in.web.trace.TraceId;
import io.opencertflow.common.domain.port.TimeProvider;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import reactor.core.publisher.Mono;

/**
 * 인증 API. 로그인하지 않은 사용자가 호출하는 엔드포인트 모음이며 시큐리티에서 permitAll이다.
 *
 * <p>요청 검증·변환과 응답 봉투 조립만 하고 비즈니스 판단은 하지 않는다(헥사고날 인바운드 어댑터).
 */
@Tag(name = "인증", description = "회원가입·로그인·토큰 재발급·소셜 로그인")
@WebAdapter
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final SignUpUseCase signUpUseCase;
    private final LoginUseCase loginUseCase;
    private final KakaoLoginUseCase kakaoLoginUseCase;
    private final GoogleLoginUseCase googleLoginUseCase;
    private final RefreshTokenUseCase refreshTokenUseCase;
    private final EmailVerificationUseCase emailVerificationUseCase;
    private final PasswordResetUseCase passwordResetUseCase;
    private final TimeProvider timeProvider;

    public AuthController(
            SignUpUseCase signUpUseCase,
            LoginUseCase loginUseCase,
            KakaoLoginUseCase kakaoLoginUseCase,
            GoogleLoginUseCase googleLoginUseCase,
            RefreshTokenUseCase refreshTokenUseCase,
            EmailVerificationUseCase emailVerificationUseCase,
            PasswordResetUseCase passwordResetUseCase,
            TimeProvider timeProvider) {
        this.signUpUseCase = signUpUseCase;
        this.loginUseCase = loginUseCase;
        this.kakaoLoginUseCase = kakaoLoginUseCase;
        this.googleLoginUseCase = googleLoginUseCase;
        this.refreshTokenUseCase = refreshTokenUseCase;
        this.emailVerificationUseCase = emailVerificationUseCase;
        this.passwordResetUseCase = passwordResetUseCase;
        this.timeProvider = timeProvider;
    }

    @PostMapping("/signup")
    public Mono<ResponseEntity<ApiResponse<AuthResponses.SignedUp>>> signUp(
            @Valid @RequestBody AuthRequests.SignUp request) {

        return signUpUseCase.signUp(
                        new SignUpUseCase.SignUpCommand(
                                request.email(), request.password(), request.nickname(),
                                request.agreedTermKeys()))
                .map(userId -> new AuthResponses.SignedUp(userId.value().toString()))
                .flatMap(body -> wrap(body, HttpStatus.CREATED));
    }

    @PostMapping("/login")
    public Mono<ResponseEntity<ApiResponse<AuthResponses.Tokens>>> login(
            @Valid @RequestBody AuthRequests.Login request) {

        return loginUseCase.login(new LoginUseCase.LoginCommand(request.email(), request.password()))
                .map(AuthResponses.Tokens::from)
                .flatMap(body -> wrap(body, HttpStatus.OK));
    }

    /** 카카오 로그인. 앱이 받은 인가 코드를 그대로 넘긴다. 신규 사용자면 이 호출로 가입까지 끝난다. */
    @PostMapping("/kakao")
    public Mono<ResponseEntity<ApiResponse<AuthResponses.Tokens>>> kakaoLogin(
            @Valid @RequestBody AuthRequests.KakaoLogin request) {

        return kakaoLoginUseCase.login(new KakaoLoginUseCase.KakaoLoginCommand(request.code()))
                .map(AuthResponses.Tokens::from)
                .flatMap(body -> wrap(body, HttpStatus.OK));
    }

    /** 구글 로그인. 앱이 받은 인가 코드를 그대로 넘긴다. 신규 사용자면 이 호출로 가입까지 끝난다. */
    @PostMapping("/google")
    public Mono<ResponseEntity<ApiResponse<AuthResponses.Tokens>>> googleLogin(
            @Valid @RequestBody AuthRequests.GoogleLogin request) {

        return googleLoginUseCase.login(new GoogleLoginUseCase.GoogleLoginCommand(request.code()))
                .map(AuthResponses.Tokens::from)
                .flatMap(body -> wrap(body, HttpStatus.OK));
    }

    @PostMapping("/refresh")
    public Mono<ResponseEntity<ApiResponse<AuthResponses.Tokens>>> refresh(
            @Valid @RequestBody AuthRequests.Refresh request) {

        return refreshTokenUseCase.refresh(new RefreshTokenUseCase.RefreshCommand(request.refreshToken()))
                .map(AuthResponses.Tokens::from)
                .flatMap(body -> wrap(body, HttpStatus.OK));
    }

    @PostMapping("/email/code")
    public Mono<ResponseEntity<ApiResponse<Void>>> sendEmailCode(
            @Valid @RequestBody AuthRequests.SendEmailCode request) {

        return emailVerificationUseCase.sendCode(request.email())
                .then(wrap(null, HttpStatus.ACCEPTED));
    }

    @PostMapping("/email/verify")
    public Mono<ResponseEntity<ApiResponse<Void>>> verifyEmail(
            @Valid @RequestBody AuthRequests.VerifyEmailCode request) {

        return emailVerificationUseCase.verify(
                        new EmailVerificationUseCase.VerifyEmailCommand(request.email(), request.code()))
                .then(wrap(null, HttpStatus.OK));
    }

    /**
     * 비밀번호 재설정 링크 요청. 가입 여부와 무관하게 202로 응답한다 — 계정 존재 여부를 노출하지
     * 않기 위함이며, 이는 서비스 계층의 정책과 짝을 이룬다.
     */
    @PostMapping("/password/reset-request")
    public Mono<ResponseEntity<ApiResponse<Void>>> requestPasswordReset(
            @Valid @RequestBody AuthRequests.RequestPasswordReset request) {

        return passwordResetUseCase.requestReset(request.email())
                .then(wrap(null, HttpStatus.ACCEPTED));
    }

    @PostMapping("/password/reset")
    public Mono<ResponseEntity<ApiResponse<Void>>> resetPassword(
            @Valid @RequestBody AuthRequests.ResetPassword request) {

        return passwordResetUseCase.reset(
                        new PasswordResetUseCase.ResetPasswordCommand(
                                request.token(), request.newPassword()))
                .then(wrap(null, HttpStatus.OK));
    }

    private <T> Mono<ResponseEntity<ApiResponse<T>>> wrap(T body, HttpStatus status) {
        return TraceId.current().map(traceId -> ResponseEntity.status(status)
                .body(ApiResponse.success(body, traceId, timeProvider.now())));
    }
}
