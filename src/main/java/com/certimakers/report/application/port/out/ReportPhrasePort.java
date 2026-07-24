package com.certimakers.report.application.port.out;

import java.util.List;

/** 리포트 문구 조회·편집. 블로킹(JPA)이라 호출자는 BlockingBridge로 감싼다. */
public interface ReportPhrasePort {

    List<Phrase> findAll();

    void upsert(String phraseKey, String text, String description);

    record Phrase(String phraseKey, String text, String description) {
    }
}
