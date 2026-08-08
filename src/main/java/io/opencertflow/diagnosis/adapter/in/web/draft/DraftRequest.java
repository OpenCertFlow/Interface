package io.opencertflow.diagnosis.adapter.in.web.draft;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotNull;

/**
 * 진단 초안 저장·수정 요청(F-APP-004).
 *
 * @param productGroup 작성 중인 제품군(선택). 목록 표시용
 * @param input        입력 원문. 미완성이어도 되며(진단 검증 안 함) JSON 객체 그대로 담는다
 */
public record DraftRequest(
        String productGroup,
        @NotNull(message = "초안 입력이 필요합니다.") JsonNode input) {
}
