package com.certimakers.common.adapter.out.persistence.json;

import com.certimakers.common.domain.error.CommonErrorCode;
import com.certimakers.common.domain.error.BusinessException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;

/**
 * jsonb 컬럼에 담기는 문자열 배열을 다루는 헬퍼. 영속성 어댑터 전용이다.
 *
 * <p>Jackson은 어댑터 계층에서만 쓴다. 도메인은 Jackson을 모른다(ArchUnit). 매퍼가 도메인
 * 컬렉션을 이 헬퍼로 JSON 문자열로 바꿔 저장하고, 읽을 때 되돌린다.
 */
public final class JsonColumns {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
    };

    private JsonColumns() {
    }

    public static String writeStringList(List<String> values) {
        try {
            return MAPPER.writeValueAsString(values);
        } catch (Exception e) {
            throw new BusinessException(CommonErrorCode.INTERNAL_ERROR, "JSON 직렬화 실패", java.util.Map.of(), e);
        }
    }

    public static List<String> readStringList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return MAPPER.readValue(json, STRING_LIST);
        } catch (Exception e) {
            throw new BusinessException(CommonErrorCode.INTERNAL_ERROR, "JSON 역직렬화 실패", java.util.Map.of(), e);
        }
    }
}
