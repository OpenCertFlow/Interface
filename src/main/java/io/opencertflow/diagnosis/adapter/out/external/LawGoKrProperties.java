package io.opencertflow.diagnosis.adapter.out.external;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * 법제처 국가법령정보 공동활용 Open API 설정.
 *
 * <p>이용하려면 <a href="https://open.law.go.kr/LSO/openApi/cuAskList.do">open.law.go.kr</a>에서
 * 신청해 {@code OC} 값을 발급받아야 한다. 기관 문서는 "이메일 ID"라고 설명하지만, 실제로는
 * <b>숫자 문자열이 발급되는 경우가 있다</b>(일반 메일 주소로 신청한 경우). 둘 다 그대로 넣으면 된다.
 *
 * <p>이 값은 <b>비밀로 다룬다.</b> 저장소에 커밋하지 않고 환경변수로만 주입한다 — 남이 쓰면
 * 우리 쿼터가 소모되고, 기관 정책 위반으로 키가 정지될 수 있다.
 *
 * @param enabled 끄면 어댑터가 항상 빈 결과를 돌려준다. 키가 없는 환경(로컬·CI)의 기본값이다.
 * @param oc      발급받은 인증값. {@code OPENCERTFLOW_LAW_OC}로 주입한다.
 * @param baseUrl 서비스 주소. https를 지원한다(확인함). 기관이 경로를 바꾸는 일이 있어 설정으로 뺀다.
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
