package io.opencertflow.auth.application.port.in;

import java.util.List;
import reactor.core.publisher.Mono;

/** 현재 약관 조회(F-AUTH-008). 회원가입 화면이 동의받을 약관을 가져간다. */
public interface GetTermsUseCase {

    Mono<List<TermView>> current();

    record TermView(String key, String version, String title, String content, boolean required) {
    }
}
