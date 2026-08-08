package io.opencertflow.diagnosis.adapter.in.web.draft;

import io.opencertflow.common.domain.error.BusinessException;
import io.opencertflow.common.domain.error.CommonErrorCode;
import io.opencertflow.diagnosis.domain.model.DiagnosisDraft;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;

/** 진단 초안 응답(F-APP-004). 저장된 payload(JSON 문자열)를 다시 JSON으로 펴서 돌려준다. */
public record DraftResponse(
        String id,
        String productGroup,
        JsonNode input,
        Instant createdAt,
        Instant updatedAt) {

    public static DraftResponse from(DiagnosisDraft draft, ObjectMapper objectMapper) {
        return new DraftResponse(
                Long.toString(draft.id()),
                draft.productGroup(),
                readTree(draft.payload(), objectMapper),
                draft.createdAt(),
                draft.updatedAt());
    }

    private static JsonNode readTree(String payload, ObjectMapper objectMapper) {
        try {
            return objectMapper.readTree(payload);
        } catch (Exception e) {
            // 저장 시 JsonNode를 직렬화한 값이라 정상 경로에서는 발생하지 않는다.
            throw new BusinessException(CommonErrorCode.INTERNAL_ERROR, "초안 payload 파싱 실패",
                    java.util.Map.of(), e);
        }
    }
}
