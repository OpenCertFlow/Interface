package io.opencertflow.diagnosis.application.port.out;

import java.util.List;

/** 준비도 가중치 조회·편집. 진단 점수 로드(LoadScoreRubricPort)와 분리된 관리 경로다. */
public interface DocumentWeightAdminPort {

    List<WeightRow> findAll();

    boolean adjust(String documentCode, int weight, String note);

    record WeightRow(String documentCode, String displayName, String requirement, int weight,
                     String note) {
    }
}
