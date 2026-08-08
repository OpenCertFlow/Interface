package io.opencertflow.auth.adapter.out.google;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** 구글 토큰 엔드포인트 응답 중 필요한 필드만. */
@JsonIgnoreProperties(ignoreUnknown = true)
record GoogleTokenResponse(@JsonProperty("access_token") String accessToken) {
}
