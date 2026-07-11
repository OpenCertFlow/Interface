package com.certimakers.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * API 명세는 산출물 목록에 포함되어 있다. 코드에서 자동 생성하여 문서와 구현이 어긋나지 않게 한다.
 * 로컬: http://localhost:8080/swagger-ui.html
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI certiMakersOpenApi() {
        return new OpenAPI().info(new Info()
                .title("인증메이커스 API")
                .version("v1")
                .description("""
                        1인 제조기업·소공인 제품 인증 준비도 자가진단 및 컨설팅 연계 서비스.

                        모든 응답은 ApiResponse 봉투로 감싸집니다.
                        준비도 점수는 인증 합격·불합격 판정이 아니라 준비 상태를 확인하기 위한 사전 점검 지표입니다.
                        """)
                .license(new License().name("팀 육성사이다")));
    }
}
