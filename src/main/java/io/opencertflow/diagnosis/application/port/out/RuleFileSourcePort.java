package io.opencertflow.diagnosis.application.port.out;

import java.util.List;

/**
 * 룰과 가중치의 <b>진실의 원천</b>인 정의 파일을 읽는다.
 *
 * <p>이 포트가 존재하는 이유는 오픈소스로서의 검증 가능성이다. 룰이 DB 안에만 있으면 커뮤니티가
 * {@code git diff}로 변경을 검토할 수 없고, "누구나 검증하고 확장할 수 있다"는 약속이 성립하지
 * 않는다. 그래서 룰은 저장소의 {@code rules/}·{@code weights/}에 파일로 살고, DB는 기동 시
 * 그 파일로 채워지는 런타임 사본이 된다.
 *
 * <p>파일 형식은 {@code schema/*.schema.json}이 정의하며, 구현체가 로드 전에 검증한다.
 */
public interface RuleFileSourcePort {

    /** 모든 룰셋 정의를 읽는다. 파일이 없으면 빈 목록(동기화를 건너뛴다). */
    List<RuleSetFile> loadRuleSets();

    /** 준비도 가중치 기준표를 읽는다. */
    List<DocumentWeightFile> loadDocumentWeights();

    /**
     * 룰을 어디서 읽었는지를 사람이 읽을 수 있게 돌려준다(예: {@code jar 내장}, {@code /etc/…}).
     * 기동 로그에 남겨서 "지금 도는 룰이 어느 파일에서 왔는가"에 답할 수 있게 한다.
     */
    String describeSource();

    /**
     * 룰셋 하나. {@code condition}·{@code effects}는 도메인 트리로 파싱하지 않고 JSON 문자열로
     * 들고 있는다 — jsonb 컬럼에 그대로 들어가고, 읽을 때 {@code RuleJsonCodec}이 도메인으로
     * 되돌린다. 로드 경로에서 한 번 더 파싱하면 같은 지식이 두 곳에 생긴다.
     */
    record RuleSetFile(
            String productGroup,
            int version,
            boolean active,
            String description,
            List<RuleFile> rules,
            String origin) {
    }

    /** 룰 하나. {@code origin}은 오류 메시지에 파일 경로를 담기 위한 것이다. */
    record RuleFile(
            String code,
            int priority,
            String description,
            String conditionJson,
            String effectsJson) {
    }

    record DocumentWeightFile(
            String documentCode,
            String displayName,
            String requirement,
            int weight,
            String note) {
    }
}
