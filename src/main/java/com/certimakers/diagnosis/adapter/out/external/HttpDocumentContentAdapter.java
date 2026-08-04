package com.certimakers.diagnosis.adapter.out.external;

import com.certimakers.common.adapter.out.external.annotation.ExternalAdapter;
import com.certimakers.diagnosis.application.port.out.FetchDocumentContentPort;
import java.time.Duration;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * 공식 문서 원문을 HTTP로 가져온다. 변경 감지 전용이다.
 *
 * <p>본문을 저장하지 않고 해시만 남기므로 여기서는 문자열을 돌려주기만 한다.
 *
 * <p>실패를 예외로 올리지 않는다. 공식 기관 사이트는 점검·차단·개편으로 흔히 실패하고, 그때마다
 * 배치가 중단되면 나머지 문서를 확인하지 못한다. 못 가져온 문서는 "확인하지 못함"으로 남을 뿐
 * "변경됨"이 되지 않는다 — 이 구분이 중요하다. 오탐은 경보를 무의미하게 만든다.
 */
@ExternalAdapter
public class HttpDocumentContentAdapter implements FetchDocumentContentPort {

    private static final Logger log = LoggerFactory.getLogger(HttpDocumentContentAdapter.class);

    /** 원문 페이지는 클 수 있다. 지문만 필요하므로 앞부분으로 충분하다. */
    private static final int MAX_BYTES = 512 * 1024;
    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private final WebClient webClient;

    public HttpDocumentContentAdapter(WebClient.Builder builder) {
        this.webClient = builder.build();
    }

    @Override
    public Optional<String> fetch(String sourceUrl) {
        if (sourceUrl == null || sourceUrl.isBlank()) {
            return Optional.empty();
        }
        try {
            String body = webClient.get()
                    .uri(sourceUrl)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(TIMEOUT)
                    .block(TIMEOUT.plusSeconds(2));

            if (body == null || body.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(body.length() > MAX_BYTES ? body.substring(0, MAX_BYTES) : body);
        } catch (RuntimeException e) {
            log.info("공식 문서 원문을 가져오지 못했습니다 — 변경 여부를 판단하지 않습니다. url={}, cause={}",
                    sourceUrl, e.toString());
            return Optional.empty();
        }
    }
}
