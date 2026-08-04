package com.certimakers.diagnosis.application.port.out;

import java.util.List;

/**
 * 룰 정의(condition/effects JSON)가 파싱 가능한지 검증하는 아웃바운드 포트.
 *
 * <p>파싱 지식(Jackson·코덱)은 영속성 어댑터에 있고, 도메인·애플리케이션은 그것을 참조하지 않는다
 * (ArchUnit). 그래서 "이 JSON이 유효한 룰인가"라는 판단을 포트로 노출해, 관리 서비스가 어댑터에
 * 직접 의존하지 않고도 배포 전 검증을 수행하게 한다.
 */
public interface RuleDefinitionValidatorPort {

    /** 각 룰 정의를 파싱 시도한다. 문제 없는 룰은 결과에 나타나지 않는다 — 빈 목록이면 모두 유효. */
    List<Issue> validate(List<Definition> definitions);

    /** 검증 대상 룰 하나. {@code ruleCode}는 오류를 어느 룰에 매핑할지 식별한다. */
    record Definition(String ruleCode, String conditionJson, String effectsJson) {
    }

    /**
     * 파싱은 되지만 <b>의미가 깨진</b> 룰을 찾는다. 절대 발동하지 않는 조건, 중복 코드,
     * 완전히 같은 조건, 효과 없는 룰, 아무도 쓰지 않는 입력 속성.
     *
     * <p>파싱에 실패한 룰은 여기서 건너뛴다 — 문법 오류는 {@link #validate}가 이미 보고했고,
     * 같은 룰을 두 번 지적하면 어느 쪽을 먼저 고쳐야 할지 흐려진다.
     */
    List<ConsistencyIssue> checkConsistency(List<Definition> definitions);

    /** 파싱 실패 항목. */
    record Issue(String ruleCode, String message) {
    }

    /**
     * 정합성 문제 하나.
     *
     * @param severity {@code ERROR}면 룰이 의도대로 동작하지 않는다. {@code WARNING}은 유지보수 위험
     * @param ruleCode 대상 룰. 비어 있으면 룰셋 전체에 대한 지적
     * @param kind     문제 종류 (UNSATISFIABLE · DUPLICATE_CODE · DUPLICATE_CONDITION 등)
     */
    record ConsistencyIssue(String severity, String ruleCode, String kind, String message) {
    }
}
