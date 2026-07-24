package com.certimakers.diagnosis.application.port.in;

import java.util.List;
import reactor.core.publisher.Mono;

/**
 * 관리자 질문(입력 항목) 관리(F-WADM-006~008). 입력 화면의 <b>프레젠테이션</b>을 편집한다 —
 * 라벨·도움말·필수 여부·표시 순서·노출 여부·선택 보기.
 *
 * <p>코드·타입·의존(dependsOn)은 진단 DTO·Attribute·룰과 묶인 타입 계약이라 편집 대상이 아니다.
 * 존재하지 않는 코드를 편집하려 하면 거부한다 — 룰이 읽을 수 없는 질문을 만들 수 없다.
 */
public interface ManageProductGroupQuestionUseCase {

    /** 제품군의 모든 입력 항목(숨김 포함)을 편집용으로 조회한다. */
    Mono<List<QuestionAdminView>> list(String productGroup);

    /** (제품군, 코드) 항목의 프레젠테이션을 갱신한다. */
    Mono<Void> update(String productGroup, String code, UpdateQuestionCommand command);

    /** 편집용 항목 뷰. {@code overridden}은 enum 기본값과 다른 오버라이드가 걸려 있는지다. */
    record QuestionAdminView(
            String code, String label, String type, boolean required,
            String dependsOn, String helpText, int displayOrder, boolean active,
            boolean overridden, List<Option> options) {
    }

    /**
     * 프레젠테이션 편집 커맨드. null 필드는 "enum 기본값으로 되돌림"을 뜻한다.
     * {@code options}가 null이면 보기도 기본값으로 되돌린다.
     */
    record UpdateQuestionCommand(
            String label, String helpText, Boolean required,
            Integer displayOrder, boolean active, List<Option> options) {
    }

    record Option(String value, String label) {
    }
}
