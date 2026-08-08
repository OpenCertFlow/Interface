package io.opencertflow.diagnosis.adapter.out.external;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * 법제처 국가법령정보 공동활용 Open API 설정.
 *
 * <p>이용하려면 <a href="https://open.law.go.kr/LSO/openApi/guideResult.do">open.law.go.kr</a>에서
 * 신청해 <b>이메일 ID</b>(예: {@code test@korea.kr}의 {@code test})를 발급받아야 한다. 이 값이
 * 인증키 역할을 한다.
 *
 * @param enabled 끄면 어댑터가 항상 빈 결과를 돌려준다. 키가 없는 환경(로컬·CI)의 기본값이다.
 * @param oc      발급받은 이메일 ID. {@code OPENCERTFLOW_LAW_OC}로 주입한다.
 * @param baseUrl 서비스 주소. 기관이 경로를 바꾸는 일이 있어 설정으로 뺀다.
 */
@ConfigurationProperties(prefix = "opencertflow.law")
public record LawGoKrProperties(
        @DefaultValue("false") boolean enabled,
        @DefaultValue("") String oc,
        @DefaultValue("https://www.law.go.kr") String baseUrl) {

    public boolean isUsable() {
        return enabled && oc != null && !oc.isBlank();
    }
}
