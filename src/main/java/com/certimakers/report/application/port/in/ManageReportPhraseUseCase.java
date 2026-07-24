package com.certimakers.report.application.port.in;

import java.util.List;
import reactor.core.publisher.Mono;

/** 리포트 문구 조회·편집(F-WADM-016). 조회는 공개, 편집은 관리자. */
public interface ManageReportPhraseUseCase {

    Mono<List<PhraseView>> list();

    Mono<Void> update(String phraseKey, String text, String description);

    record PhraseView(String key, String text, String description) {
    }
}
