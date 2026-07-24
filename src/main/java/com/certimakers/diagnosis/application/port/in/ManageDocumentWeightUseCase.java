package com.certimakers.diagnosis.application.port.in;

import java.util.List;
import reactor.core.publisher.Mono;

/** 관리자 준비도 가중치 관리(F-WADM-011). 가중치·비고 편집. */
public interface ManageDocumentWeightUseCase {

    Mono<List<WeightView>> list();

    Mono<Void> update(String documentCode, int weight, String note);

    record WeightView(String documentCode, String displayName, String requirement, int weight,
                      String note) {
    }
}
