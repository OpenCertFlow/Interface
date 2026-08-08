package io.opencertflow.auth.application.port.out;

import java.time.Instant;
import java.util.List;

/** 약관 조회·동의 기록. 블로킹(JPA)이라 호출자는 BlockingBridge로 감싼다. */
public interface TermsPort {

    List<Term> loadActive();

    void saveAgreements(Long userId, List<AgreedTerm> agreed, Instant agreedAt);

    record Term(String termKey, String version, String title, String content, boolean required) {
    }

    record AgreedTerm(String termKey, String version) {
    }
}
