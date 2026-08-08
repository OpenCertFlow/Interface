package io.opencertflow.auth.adapter.out.google;

import io.opencertflow.auth.application.port.out.GoogleClientPort.GoogleProfile;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 구글 사용자 정보(OpenID Connect userinfo) 응답. 필요한 필드만 뽑는다.
 *
 * <p>고유 식별자는 {@code sub}이며 이메일 재사용·변경과 무관하게 안정적이다 — 그래서 계정 매칭의
 * 기준으로 이메일이 아니라 {@code sub}를 쓴다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
record GoogleUserResponse(String sub, String email, String name) {

    GoogleProfile toProfile() {
        return new GoogleProfile(sub, email, name);
    }
}
