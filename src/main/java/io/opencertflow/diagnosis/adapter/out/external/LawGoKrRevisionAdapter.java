package io.opencertflow.diagnosis.adapter.out.external;

import com.fasterxml.jackson.databind.JsonNode;
import io.opencertflow.common.adapter.out.external.annotation.ExternalAdapter;
import io.opencertflow.diagnosis.application.port.out.LawRevisionPort;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * 법제처 국가법령정보 공동활용 Open API로 법령 개정 이력을 읽는다.
 *
 * <p>엔드포인트는 {@code /DRF/lawSearch.do?OC=<이메일ID>&target=law&type=JSON&query=<법령명>}이며,
 * 응답의 {@code LawSearch.law[]}에 공포일자·공포번호·시행일자가 들어 있다.
 *
 * <p><b>키가 없으면 조용히 비활성이다.</b> 오픈소스 저장소를 클론한 사람이 인증키 없이도 빌드·테스트를
 * 돌릴 수 있어야 하고, 키가 없다는 이유로 진단 기능이 막히면 안 된다. 신선도 감지는 부가 기능이다.
 *
 * <p><b>이 어댑터는 실제 API 응답으로 검증되지 않았다.</b> 인증키 발급이 선행되어야 한다
 * ({@code open.law.go.kr} → OPEN API 신청). 필드명이 문서와 다르면 {@link #parse}만 고치면 된다 —
 * 그래서 파싱을 한 메서드에 몰아 두었다.
 */
@ExternalAdapter
public class LawGoKrRevisionAdapter implements LawRevisionPort {

    private static final Logger log = LoggerFactory.getLogger(LawGoKrRevisionAdapter.class);
    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    /** 국가법령정보 API는 날짜를 구분자 없는 8자리로 준다(예: 20260301). */
    private static final DateTimeFormatter YYYYMMDD = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final WebClient webClient;
    private final LawGoKrProperties properties;

    public LawGoKrRevisionAdapter(WebClient.Builder builder, LawGoKrProperties properties) {
        this.webClient = builder.build();
        this.properties = properties;
    }

    @Override
    public Optional<LawRevision> findCurrent(String lawName) {
        if (!properties.isUsable()) {
            log.debug("법제처 API 키가 없어 개정 조회를 건너뜁니다. (opencertflow.law.oc)");
            return Optional.empty();
        }
        if (lawName == null || lawName.isBlank()) {
            return Optional.empty();
        }

        try {
            JsonNode body = webClient.get()
                    .uri(searchUri(lawName))
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .timeout(TIMEOUT)
                    .block(TIMEOUT.plusSeconds(2));

            return parse(body, lawName);
        } catch (RuntimeException e) {
            log.info("법제처 개정 조회 실패 — 변경 여부를 판단하지 않습니다. law={}, cause={}",
                    lawName, e.toString());
            return Optional.empty();
        }
    }

    private String searchUri(String lawName) {
        return UriComponentsBuilder.fromUriString(properties.baseUrl())
                .path("/DRF/lawSearch.do")
                .queryParam("OC", properties.oc())
                .queryParam("target", "law")
                .queryParam("type", "JSON")
                .queryParam("display", 1)
                .queryParam("query", lawName)
                .build()
                .encode()
                .toUriString();
    }

    /**
     * 응답에서 첫 법령의 개정 정보를 꺼낸다.
     *
     * <p>필드명이 기관 문서와 어긋나면 여기만 고치면 된다. 값이 없거나 형식이 다르면 그 필드는
     * {@code null}로 두고 나머지는 살린다 — 시행일 하나 못 읽었다고 개정 사실 자체를 버릴 이유가 없다.
     */
    private Optional<LawRevision> parse(JsonNode body, String requestedName) {
        if (body == null) {
            return Optional.empty();
        }
        JsonNode laws = body.path("LawSearch").path("law");
        JsonNode first = laws.isArray() ? (laws.isEmpty() ? null : laws.get(0)) : laws;
        if (first == null || first.isMissingNode() || first.isNull()) {
            log.info("법제처 응답에 법령이 없습니다. law={}", requestedName);
            return Optional.empty();
        }

        return Optional.of(new LawRevision(
                text(first, "법령명한글", requestedName),
                text(first, "공포번호", null),
                date(first, "공포일자"),
                date(first, "시행일자"),
                text(first, "법령상세링크", null)));
    }

    private String text(JsonNode node, String field, String fallback) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull() || value.asText().isBlank()) {
            return fallback;
        }
        return value.asText().trim();
    }

    private LocalDate date(JsonNode node, String field) {
        String raw = text(node, field, null);
        if (raw == null) {
            return null;
        }
        try {
            return LocalDate.parse(raw.replace("-", "").trim(), YYYYMMDD);
        } catch (DateTimeParseException e) {
            log.debug("법제처 날짜 형식을 해석하지 못했습니다. field={}, value={}", field, raw);
            return null;
        }
    }
}
