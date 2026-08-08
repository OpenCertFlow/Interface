package io.opencertflow.auth.adapter.out.kakao;

import io.opencertflow.auth.application.port.out.KakaoClientPort.KakaoProfile;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 카카오 사용자 정보 응답. 필요한 필드(회원번호·이메일·닉네임)만 뽑는다.
 *
 * <p>이메일은 {@code kakao_account.email}, 닉네임은 {@code kakao_account.profile.nickname}에 있으며
 * 사용자가 동의하지 않으면 null이다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
record KakaoUserResponse(
        Long id,
        @JsonProperty("kakao_account") KakaoAccount kakaoAccount) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    record KakaoAccount(String email, Profile profile) {

        @JsonIgnoreProperties(ignoreUnknown = true)
        record Profile(String nickname) {
        }
    }

    KakaoProfile toProfile() {
        String email = kakaoAccount != null ? kakaoAccount.email() : null;
        String nickname = kakaoAccount != null && kakaoAccount.profile() != null
                ? kakaoAccount.profile().nickname()
                : null;
        return new KakaoProfile(String.valueOf(id), email, nickname);
    }
}
