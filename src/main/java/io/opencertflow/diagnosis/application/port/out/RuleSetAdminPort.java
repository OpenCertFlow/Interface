package io.opencertflow.diagnosis.application.port.out;

import io.opencertflow.diagnosis.domain.model.ProductGroup;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * 관리자 룰셋 조회·저장·배포 아웃바운드 포트. 블로킹(JPA)이므로 호출자는 {@code BlockingBridge}로 감싼다.
 *
 * <p>{@link LoadRuleSetPort}(활성 룰셋 로드, 진단 경로)와 분리한 이유는 관심사가 다르기 때문이다.
 * 진단은 "지금 활성인 룰셋"만 파싱된 도메인 형태로 필요하지만, 관리 화면은 <b>모든 버전</b>을
 * 저장된 JSON 원문 그대로 보고 편집·배포한다.
 */
public interface RuleSetAdminPort {

    /** 모든 룰셋 요약. 정렬은 어댑터가 정한다(제품군·버전 내림차순). */
    List<RuleSetSummary> findAllSummaries();

    /** 룰셋 상세(룰의 저장 JSON 포함). 없으면 비어 있음. */
    Optional<RuleSetDetail> findDetail(Long ruleSetId);

    /** 제품군의 다음 버전 번호. 기존 최대 버전 + 1, 없으면 1. */
    int nextVersion(ProductGroup productGroup);

    /** 새 비활성 룰셋을 저장하고 생성된 id를 돌려준다. */
    Long saveDraft(NewRuleSet ruleSet);

    /**
     * 룰셋을 활성화한다. 같은 제품군의 기존 활성 룰셋을 먼저 비활성화한 뒤 대상을 활성화한다.
     * 대상이 없으면 false.
     */
    boolean activate(Long ruleSetId);

    // ── DTO ──────────────────────────────────────────────────────

    record RuleSetSummary(Long id, String productGroup, int version, boolean active,
                          Instant activatedAt, int ruleCount) {
    }

    record RuleSetDetail(Long id, String productGroup, int version, boolean active,
                         Instant activatedAt, List<StoredRule> rules) {
    }

    record StoredRule(String ruleCode, int priority, String conditionJson, String effectsJson,
                      String description) {
    }

    record NewRuleSet(ProductGroup productGroup, int version, List<NewRule> rules) {
    }

    record NewRule(String ruleCode, int priority, String conditionJson, String effectsJson,
                   String description) {
    }
}
