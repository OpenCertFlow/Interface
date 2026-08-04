package com.certimakers.diagnosis.application.port.in;

import java.time.Instant;
import java.util.List;
import reactor.core.publisher.Mono;

/**
 * 관리자 룰셋 관리 유스케이스(F-WADM-009 규칙 관리, F-WADM-010 검증·배포).
 *
 * <p>지금까지 룰은 {@code R__seed_rules*.sql}로만 주입됐다 — 규칙을 바꾸려면 SQL을 고치고 재배포해야
 * 했다. 이 유스케이스는 그 과정을 API로 옮겨, 관리자가 <b>초안 작성 → 검증 → 배포(활성화)</b>를
 * 화면에서 수행하도록 한다.
 *
 * <p>안전장치: 배포 전 항상 룰 정의(JSON)를 파싱 검증한다. 잘못된 condition/effects가 활성 룰셋에
 * 들어가면 진단 전체가 깨지므로, 저장·배포 경로에서 파싱 실패를 먼저 막는다.
 */
public interface ManageRuleSetUseCase {

    /** 모든 룰셋 요약(버전·활성 여부·룰 수). 최신 버전이 위로. */
    Mono<List<RuleSetSummary>> list();

    /** 룰셋 상세 — 각 룰의 condition/effects를 저장된 JSON 그대로 보여 준다(편집·검토용). */
    Mono<RuleSetDetail> get(Long ruleSetId);

    /**
     * 룰 정의를 파싱 검증만 한다(저장하지 않음). 배포 전에 관리자가 안전하게 확인하는 용도다.
     * 문제가 없으면 {@code issues}가 비어 있다.
     */
    Mono<ValidationResult> validate(List<RuleDraft> rules);

    /**
     * 새 룰셋 초안을 만든다(비활성). 제품군의 다음 버전 번호를 자동 부여한다. 저장 전에 모든 룰을
     * 검증하며, 하나라도 파싱 실패면 저장하지 않고 검증 오류를 돌려준다.
     */
    Mono<Long> createDraft(CreateRuleSetCommand command);

    /**
     * 룰셋을 활성화(배포)한다. 같은 제품군의 기존 활성 룰셋은 자동으로 비활성화된다 —
     * "제품군당 활성 룰셋 하나" 불변식(부분 유니크 인덱스)을 지킨다.
     */
    Mono<Void> activate(Long ruleSetId);

    // ── 커맨드/뷰 ────────────────────────────────────────────────

    /** 룰 한 건의 정의. condition/effects는 RuleJsonCodec이 파싱하는 JSON 문자열이다. */
    record RuleDraft(String ruleCode, int priority, String conditionJson, String effectsJson,
                     String description) {
    }

    record CreateRuleSetCommand(String productGroup, List<RuleDraft> rules) {
    }

    record RuleSetSummary(Long id, String productGroup, int version, boolean active,
                          Instant activatedAt, int ruleCount) {
    }

    record RuleSetDetail(Long id, String productGroup, int version, boolean active,
                         Instant activatedAt, List<RuleLine> rules) {
    }

    record RuleLine(String ruleCode, int priority, String conditionJson, String effectsJson,
                    String description) {
    }

    /**
     * 검증 결과.
     *
     * @param valid       문법 오류와 정합성 ERROR가 모두 없으면 참. 배포 가능 여부다
     * @param issues      문법(파싱) 오류
     * @param consistency 의미 검사 결과. WARNING은 배포를 막지 않는다
     */
    record ValidationResult(
            boolean valid, List<RuleIssue> issues, List<ConsistencyIssue> consistency) {

        /** 정합성 검사가 없던 호출부를 그대로 두기 위한 생성자. */
        public ValidationResult(boolean valid, List<RuleIssue> issues) {
            this(valid, issues, List.of());
        }
    }

    /** @param severity ERROR면 배포를 막는다. WARNING은 알리기만 한다 */
    record ConsistencyIssue(String severity, String ruleCode, String kind, String message) {
    }

    /** 룰 하나의 검증 오류. {@code ruleCode}가 어느 룰인지 가리킨다. */
    record RuleIssue(String ruleCode, String message) {
    }
}
