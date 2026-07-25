package com.certimakers.auth.application.port.out;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** 약관 조회·동의 기록. 블로킹(JPA)이라 호출자는 BlockingBridge로 감싼다. */
public interface TermsPort {

    List<Term> loadActive();

    void saveAgreements(UUID userId, List<AgreedTerm> agreed, Instant agreedAt);

    record Term(String termKey, String version, String title, String content, boolean required) {
    }

    record AgreedTerm(String termKey, String version) {
    }
}
