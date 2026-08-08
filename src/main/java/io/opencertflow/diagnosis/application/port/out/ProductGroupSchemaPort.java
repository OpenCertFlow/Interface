package io.opencertflow.diagnosis.application.port.out;

import java.util.List;
import java.util.Optional;

/**
 * 제품군 입력 항목의 프레젠테이션 오버라이드 저장·조회 아웃바운드 포트. 블로킹(JPA)이므로 호출자는
 * {@code BlockingBridge}로 감싼다.
 *
 * <p>코드/타입/의존은 {@code ProductGroup} enum(타입 계약)이 갖고, 이 포트는 그 위에 얹는
 * 프레젠테이션 델타만 다룬다. 오버라이드가 없으면 enum 기본값이 그대로 유효하다.
 */
public interface ProductGroupSchemaPort {

    /** 제품군의 모든 오버라이드. 없으면 빈 목록 → 전부 enum 기본값이 유효하다. */
    List<QuestionOverride> loadOverrides(String productGroup);

    /** (제품군, 코드)의 오버라이드. */
    Optional<QuestionOverride> findOverride(String productGroup, String code);

    /** 오버라이드를 저장/갱신(upsert)한다. */
    void upsert(String productGroup, String code, OverridePatch patch);

    /**
     * 프레젠테이션 오버라이드 값. 각 필드가 null이면 그 항목은 enum 기본값을 쓴다.
     * {@code options}가 null이면 보기도 enum 기본값을 쓴다.
     */
    record QuestionOverride(
            String code, String label, String helpText, Boolean required,
            Integer displayOrder, boolean active, List<Option> options) {
    }

    record OverridePatch(
            String label, String helpText, Boolean required,
            Integer displayOrder, boolean active, List<Option> options) {
    }

    record Option(String value, String label) {
    }
}
