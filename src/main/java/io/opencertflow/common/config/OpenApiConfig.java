package io.opencertflow.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * API 명세는 산출물 목록에 포함되어 있다. 코드에서 자동 생성하여 문서와 구현이 어긋나지 않게 한다.
 * 로컬: http://localhost:8080/swagger-ui.html
 *
 * <p><b>JWT 인증 스키마</b>를 등록해 Swagger UI에 "Authorize" 버튼이 뜨게 한다. 심사·시연에서
 * 로그인 응답의 accessToken을 넣으면 인증이 필요한 엔드포인트(내 진단·초안·마이페이지 등)를 그대로
 * 눌러볼 수 있다. 비로그인으로 열린 엔드포인트는 토큰이 있어도 무시하므로 문제되지 않는다.
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI openCertFlowOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("OpenCertFlow API")
                        .version("v1")
                        .description("""
                                1인 제조기업·소공인 제품 인증 준비도 자가진단 및 컨설팅 연계 서비스.

                                • 모든 응답은 ApiResponse 봉투(success/data/error/traceId/timestamp)로 감쌉니다.
                                • 준비도 점수는 인증 합격·불합격 판정이 아니라 준비 상태를 확인하는 사전 점검 지표입니다.
                                • 인증이 필요한 엔드포인트는 우상단 Authorize에 로그인 accessToken을 넣고 호출하세요.
                                """)
                        .license(new License().name("팀 육성사이다")))
                .components(new Components().addSecuritySchemes(BEARER_SCHEME,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("로그인(POST /api/v1/auth/login) 응답의 accessToken")))
                // 전역 요구로 걸어 Authorize 후 토큰이 모든 요청에 실린다. 공개 엔드포인트는 토큰을 무시한다.
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
    }
}
