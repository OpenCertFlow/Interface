package io.opencertflow.diagnosis.domain.model;

import java.time.Instant;

/**
 * 진단 입력 초안(F-APP-004). 아직 진단을 실행하지 않은, 저장해 두고 이어서 작성하는 제품 입력이다.
 *
 * <p>완성된 진단({@link Diagnosis})과 달리 <b>미완성일 수 있다</b> — 그래서 {@link ProductProfile}로
 * 검증·정규화하지 않고 입력 원문(JSON)을 그대로 담는다. 검증은 실제 진단 실행 시점에 이뤄진다.
 *
 * @param id           초안 식별자(생성 시 앱이 부여)
 * @param ownerUserId  초안 소유자(로그인 사용자). 초안은 항상 소유자가 있다
 * @param productGroup 작성 중인 제품군(목록 표시용). 아직 못 정했으면 null
 * @param payload      입력 원문(JSON 문자열). 미완성 입력도 그대로 보존한다
 * @param createdAt    생성 시각
 * @param updatedAt    마지막 수정 시각
 */
public record DiagnosisDraft(
        Long id,
        String ownerUserId,
        String productGroup,
        String payload,
        Instant createdAt,
        Instant updatedAt) {
}
