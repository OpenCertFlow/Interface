package com.certimakers.diagnosis.adapter.in.web;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;

/**
 * 시뮬레이션 요청. 모든 필드가 선택이며, <b>null은 "그 속성은 원본 그대로"</b>를 뜻한다.
 *
 * <p>화면에서 체크박스 하나만 켜는 상황을 그대로 표현하기 위한 부분 변경 형태다.
 *
 * @param addDocuments      추가로 보유했다고 가정할 서류 코드
 * @param removeDocuments   보유하지 않았다고 가정할 서류 코드
 * @param usesElectricity   전기 사용 여부
 * @param ratedVoltage      정격전압(V)
 * @param powerConsumption  소비전력(W)
 * @param hasBattery        배터리 내장 여부
 * @param targetUser        사용 대상 (GENERAL·CHILD·INDUSTRIAL)
 * @param salesChannel      판매 방식 (ONLINE·OFFLINE·BOTH)
 */
public record SimulateRequest(
        List<String> addDocuments,
        List<String> removeDocuments,
        Boolean usesElectricity,
        @Min(value = 0, message = "정격전압은 0 이상이어야 합니다.")
        @Max(value = 100_000, message = "정격전압이 허용 범위를 벗어났습니다.")
        Integer ratedVoltage,
        @Min(value = 0, message = "소비전력은 0 이상이어야 합니다.")
        @Max(value = 1_000_000, message = "소비전력이 허용 범위를 벗어났습니다.")
        Integer powerConsumption,
        Boolean hasBattery,
        String targetUser,
        String salesChannel) {

    public SimulateRequest {
        addDocuments = addDocuments == null ? List.of() : List.copyOf(addDocuments);
        removeDocuments = removeDocuments == null ? List.of() : List.copyOf(removeDocuments);
    }
}
