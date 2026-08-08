package io.opencertflow.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import java.security.Principal;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;

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

    static {
        // 프레임워크가 주입하는 인증 객체를 명세에서 제외한다.
        //
        // springdoc은 컨트롤러의 {@code Principal}·{@code Authentication} 파라미터를 클라이언트가
        // 채워야 하는 <b>쿼리 파라미터</b>로 오해해 명세에 넣는다. 그러면 Swagger UI에 존재하지
        // 않는 입력란이 뜨고, 생성된 SDK에는 아무도 쓸 수 없는 인자가 생긴다.
        //
        // <p><b>이 설정만으로는 부족하다.</b> 이 프로젝트의 컨트롤러는 대부분 {@code Mono<Principal>}
        // 형태로 받는데, springdoc은 그 리액티브 래퍼를 벗기지 않아 여기 등록한 타입과 매칭하지
        // 못한다. 그래서 해당 파라미터에는 {@code @Parameter(hidden = true)}를 직접 붙였다.
        // 여기 등록은 래핑되지 않은 경우를 위한 것이다.
        SpringDocUtils.getConfig().addRequestWrapperToIgnore(Principal.class, Authentication.class);
    }

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
