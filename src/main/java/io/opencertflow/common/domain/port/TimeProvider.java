package io.opencertflow.common.domain.port;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * 현재 시각 제공. 아웃바운드 포트다.
 *
 * <p>도메인이 {@code Instant.now()}를 직접 부르면 그 순간 룰 평가가 순수 함수이기를 멈춘다.
 * 테스트에서 시각을 고정할 수 없고, "동일 입력 → 동일 결과"를 검증할 수 없다.
 */
public interface TimeProvider {

    Instant now();

    ZoneId zone();

    default LocalDate today() {
        return LocalDate.ofInstant(now(), zone());
    }
}
