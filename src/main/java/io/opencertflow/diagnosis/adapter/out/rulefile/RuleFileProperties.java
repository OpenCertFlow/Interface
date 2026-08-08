package io.opencertflow.diagnosis.adapter.out.rulefile;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * 룰 파일 로딩 설정.
 *
 * @param syncEnabled 기동 시 파일을 저장소에 반영할지. 끄면 DB의 기존 룰을 그대로 쓴다.
 * @param path        외부 룰 디렉터리. 비어 있으면 jar에 포함된 {@code rules/}를 쓴다.
 *                    커뮤니티가 자신의 룰 저장소를 체크아웃해 가리킬 때 쓰는 값이다.
 *                    예: {@code OPENCERTFLOW_RULES_PATH=/etc/opencertflow/rules}
 * @param weightsPath 외부 가중치 디렉터리. 비어 있으면 jar에 포함된 {@code weights/}를 쓴다.
 */
@ConfigurationProperties(prefix = "opencertflow.rules")
public record RuleFileProperties(
        @DefaultValue("true") boolean syncEnabled,
        @DefaultValue("") String path,
        @DefaultValue("") String weightsPath) {

    public boolean hasExternalRulePath() {
        return path != null && !path.isBlank();
    }

    public boolean hasExternalWeightsPath() {
        return weightsPath != null && !weightsPath.isBlank();
    }
}
