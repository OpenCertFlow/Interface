package io.opencertflow.diagnosis.adapter.out.external;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * 제품안전정보센터(safetykorea) Open API 설정.
 *
 * <p>인증키는 자동 발급이 아니다. <a href="https://www.safetykorea.kr/release/openapi2">신청 안내</a>에서
 * 신청서를 내려받아 작성한 뒤 {@code safetykorea@korea.kr}로 보내면 승인 후 이메일로 온다.
 *
 * <p>키는 <b>비밀로 다룬다.</b> 기관 정책상 타인·타기관에 양도할 수 없으며, 저장소에 커밋하지 않고
 * 환경변수로만 주입한다.
 *
 * @param enabled 끄면 어댑터가 항상 빈 결과를 돌려준다. 키가 없는 환경(로컬·CI)의 기본값이다.
 * @param authKey 발급받은 인증키. {@code OPENCERTFLOW_SAFETYKOREA_KEY}로 주입한다.
 *                <b>쿼리 파라미터가 아니라 {@code AuthKey} 헤더로 보낸다</b> — 쿼리로 보내면
 *                무시되고 {@code 4000 invalid Auth Key}가 난다(실제로 확인함).
 * @param baseUrl 서비스 주소.
 */
@ConfigurationProperties(prefix = "opencertflow.safetykorea")
public record SafetyKoreaProperties(
        @DefaultValue("false") boolean enabled,
        @DefaultValue("") String authKey,
        @DefaultValue("https://www.safetykorea.kr") String baseUrl) {

    public boolean isUsable() {
        return enabled && authKey != null && !authKey.isBlank();
    }
}
