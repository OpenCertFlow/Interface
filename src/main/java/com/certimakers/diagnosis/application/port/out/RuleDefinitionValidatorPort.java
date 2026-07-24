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

    /** 파싱 실패 항목. */
    record Issue(String ruleCode, String message) {
    }
}
