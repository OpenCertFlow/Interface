package io.opencertflow.diagnosis.adapter.in.web.prep;

import jakarta.validation.constraints.NotNull;

/**
 * 항목 체크·해제 요청.
 *
 * <p>원시 {@code boolean}이 아니라 {@code Boolean}이다 — 원시 타입이면 값을 안 보내도 false로
 * 채워져 "해제 요청"과 구별되지 않는다. null이 될 수 있어야 {@code @NotNull}이 일한다.
 */
public record PrepItemCheckRequest(@NotNull Boolean done) {
}
