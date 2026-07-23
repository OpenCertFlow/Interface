package com.certimakers.document.adapter.in.web;

import jakarta.validation.constraints.NotBlank;
import java.util.Map;

/** 문서 발급 API 요청 DTO. */
public final class DocumentRequests {

    private DocumentRequests() {
    }

    /**
     * @param templateCode 양식 코드 (예: SELF_DECLARATION)
     * @param values       항목 코드 → 값. 항목 검증은 도메인이 양식 정의에 비추어 수행한다
     */
    public record Issue(
            @NotBlank(message = "양식 코드가 필요합니다.")
            String templateCode,
            Map<String, String> values) {

        public Issue {
            values = values == null ? Map.of() : Map.copyOf(values);
        }
    }
}
