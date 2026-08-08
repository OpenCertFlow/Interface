package io.opencertflow.auth.adapter.out.kakao;

import com.fasterxml.jackson.annotation.JsonProperty;

/** 카카오 토큰 엔드포인트 응답 중 필요한 필드만. */
record KakaoTokenResponse(@JsonProperty("access_token") String accessToken) {
}
