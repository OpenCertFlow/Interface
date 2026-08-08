package io.opencertflow.diagnosis.adapter.out.external;

import com.fasterxml.jackson.databind.JsonNode;
import io.opencertflow.common.adapter.out.external.annotation.ExternalAdapter;
import io.opencertflow.diagnosis.application.port.out.CertificationRegistryPort;
import java.net.URI;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * 제품안전정보센터 Open API로 KC 인증 등록 현황을 읽는다.
 *
 * <p><b>규격이 공개되어 있지 않다.</b> 아래는 기관이 배포하는 {@code Open_API_사용설명서.hwp}에서
 * 확인하고 실제 호출로 검증한 내용이다. 문서 링크가 바뀌어도 잊히지 않도록 여기에 남긴다.
 *
 * <pre>
 * GET {base}/openapi/api/cert/certificationList.json
 *       ?conditionKey=productName&conditionValue=전기방석
 * Header: AuthKey: &lt;인증키&gt;
 *
 * → {"resultCode":"2000","resultMsg":"Success","resultData":[ ... ]}
 * </pre>
 *
 * <p>주의할 점 셋:
 * <ul>
 *   <li>인증은 <b>헤더</b>({@code AuthKey})다. 쿼리 파라미터로 보내면 무시되고 {@code 4000}이 난다.
 *   <li>{@code conditionKey}와 {@code conditionValue}는 <b>쌍</b>이다. 하나만 주면 {@code 4005}다.
 *   <li>오류도 HTTP 200으로 온다. 상태 코드가 아니라 {@code resultCode}를 봐야 한다.
 * </ul>
 *
 * <p>실패는 예외가 아니라 빈 목록이다. 이 조회는 룰을 <b>만들 때</b> 쓰는 참고 자료이고, 진단의
 * 판정은 룰이 한다 — 기관 API가 죽었다고 진단이 멈출 이유가 없다.
 */
@ExternalAdapter
public class SafetyKoreaCertificationAdapter implements CertificationRegistryPort {

    private static final Logger log =
            LoggerFactory.getLogger(SafetyKoreaCertificationAdapter.class);

    private static final Duration TIMEOUT = Duration.ofSeconds(20);
    private static final String SUCCESS = "2000";
    private static final DateTimeFormatter YYYYMMDD = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * 응답 본문 상한 8MB.
     *
     * <p>기본값(256KB)으로는 부족하다 — "전기방석" 한 건만 조회해도 846건에 620KB가 온다. 이
     * API에는 페이지 파라미터가 없어서 품목의 전체 인증 이력이 한 번에 내려온다. 한도를 넘으면
     * {@code DataBufferLimitException}이 나는데, 그것이 아래 {@code catch}에 삼켜지면 "조회는
     * 성공했는데 결과가 0건"과 구별되지 않는다. 실제로 그렇게 한 번 속았다.
     */
    private static final int MAX_RESPONSE_BYTES = 8 * 1024 * 1024;

    private final WebClient webClient;
    private final SafetyKoreaProperties properties;

    public SafetyKoreaCertificationAdapter(
            WebClient.Builder builder, SafetyKoreaProperties properties) {
        this.webClient = builder
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(MAX_RESPONSE_BYTES))
                .build();
        this.properties = properties;
    }

    @Override
    public List<CertificationRecord> findByProductName(String productName) {
        if (!properties.isUsable()) {
            log.debug("safetykorea 인증키가 없어 인증 현황 조회를 건너뜁니다. "
                    + "(opencertflow.safetykorea.auth-key)");
            return List.of();
        }
        if (productName == null || productName.isBlank()) {
            return List.of();
        }

        try {
            JsonNode body = webClient.get()
                    .uri(searchUri(productName))
                    // 쿼리가 아니라 헤더다. 이 한 줄이 인증의 전부다.
                    .header("AuthKey", properties.authKey())
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .timeout(TIMEOUT)
                    .block(TIMEOUT.plusSeconds(2));

            return parse(body, productName);
        } catch (RuntimeException e) {
            // warn으로 남긴다. 여기서 조용히 빈 목록을 돌려주면 "조회 성공, 결과 0건"과 구별되지
            // 않아, 실제로는 깨진 연동을 정상으로 오해하게 된다.
            log.warn("safetykorea 인증 현황 조회 실패 — 빈 결과로 진행합니다. product={}, cause={}",
                    productName, e.toString());
            return List.of();
        }
    }

    /**
     * {@link URI}로 넘기는 것이 중요하다. {@code String} 오버로드는 이미 인코딩된 값을 한 번 더
     * 인코딩해 한글 검색어가 깨지고, 서버는 오류 대신 빈 결과를 돌려준다.
     */
    private URI searchUri(String productName) {
        return UriComponentsBuilder.fromUriString(properties.baseUrl())
                .path("/openapi/api/cert/certificationList.json")
                .queryParam("conditionKey", "productName")
                .queryParam("conditionValue", productName)
                .build()
                .encode()
                .toUri();
    }

    private List<CertificationRecord> parse(JsonNode body, String requested) {
        if (body == null) {
            return List.of();
        }
        // 오류도 HTTP 200으로 오므로 resultCode를 반드시 확인한다.
        String resultCode = body.path("resultCode").asText();
        if (!SUCCESS.equals(resultCode)) {
            log.info("safetykorea 응답 오류 — product={}, resultCode={}, resultMsg={}",
                    requested, resultCode, body.path("resultMsg").asText());
            return List.of();
        }

        JsonNode data = body.path("resultData");
        if (!data.isArray()) {
            return List.of();
        }

        List<CertificationRecord> records = new ArrayList<>(data.size());
        for (JsonNode node : data) {
            String division = text(node, "certDiv");
            records.add(new CertificationRecord(
                    text(node, "certNum"),
                    gradeOf(division),
                    division,
                    text(node, "categoryName"),
                    text(node, "productName"),
                    date(node, "certDate"),
                    text(node, "certOrganName")));
        }
        return List.copyOf(records);
    }

    /**
     * {@code certDiv}에서 등급을 읽는다.
     *
     * <p>값은 {@code "전기용품 및 생활용품 안전관리법 대상>안전인증대상 전기용품"}처럼 법령과 등급이
     * 한 문자열에 붙어 있다. <b>순서가 중요하다</b> — "자율안전확인"이 "안전확인"을 포함하므로
     * 좁은 것부터 본다.
     */
    private CertificationGrade gradeOf(String division) {
        if (division == null) {
            return CertificationGrade.UNKNOWN;
        }
        if (division.contains("공급자적합성") || division.contains("자율안전확인")) {
            return CertificationGrade.SUPPLIER_CONFIRMATION;
        }
        if (division.contains("안전인증")) {
            return CertificationGrade.SAFETY_CERTIFICATION;
        }
        if (division.contains("안전확인")) {
            return CertificationGrade.SAFETY_CONFIRMATION;
        }
        return CertificationGrade.UNKNOWN;
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull() || value.asText().isBlank()) {
            return null;
        }
        return value.asText().trim();
    }

    private LocalDate date(JsonNode node, String field) {
        String raw = text(node, field);
        if (raw == null) {
            return null;
        }
        try {
            return LocalDate.parse(raw.replace("-", ""), YYYYMMDD);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
