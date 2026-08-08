package io.opencertflow.diagnosis.adapter.out.rulefile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import io.opencertflow.diagnosis.application.port.out.RuleFileSourcePort;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

/**
 * {@code rules/**\/*.yaml}과 {@code weights/document-weights.yaml}을 읽는 어댑터.
 *
 * <p><b>읽기 전에 JSON Schema로 검증한다.</b> 커뮤니티가 보낸 PR의 룰 파일이 형식을 어겼을 때,
 * 애매한 NPE가 아니라 "어느 파일 어느 위치가 왜 틀렸는지"를 말해야 한다. 스키마는
 * {@code schema/ruleset.schema.json}이며 같은 파일이 IDE 자동완성에도 쓰인다.
 *
 * <p>스키마가 잡지 못하는 것 — 속성과 값 타입의 정합성, 절대 발동하지 않는 조건, 중복 룰 코드 —
 * 은 {@code RuleConsistencyChecker}와 {@code opencertflow validate}가 본다. 여기서는 구조만 본다.
 *
 * <p>기본은 jar에 포함된 파일을 읽고, {@code opencertflow.rules.path}가 설정되면 그 디렉터리를
 * 대신 읽는다. 후자가 "자신의 룰셋으로 확장한다"를 가능하게 하는 지점이다.
 */
@Component
public class YamlRuleFileAdapter implements RuleFileSourcePort {

    private static final String RULE_CLASSPATH_PATTERN = "classpath*:rules/**/*.y*ml";
    private static final String WEIGHTS_CLASSPATH = "classpath:weights/document-weights.yaml";
    private static final String RULESET_SCHEMA = "/schema/ruleset.schema.json";
    private static final String WEIGHTS_SCHEMA = "/schema/document-weights.schema.json";

    private final ObjectMapper yaml = new ObjectMapper(new YAMLFactory());
    private final ObjectMapper json = new ObjectMapper();
    private final PathMatchingResourcePatternResolver resolver =
            new PathMatchingResourcePatternResolver();

    private final RuleFileProperties properties;

    public YamlRuleFileAdapter(RuleFileProperties properties) {
        this.properties = properties;
    }

    // ── 룰셋 ─────────────────────────────────────────────────────

    @Override
    public List<RuleSetFile> loadRuleSets() {
        JsonSchema schema = loadSchema(RULESET_SCHEMA);
        List<RuleSetFile> ruleSets = new ArrayList<>();

        for (SourceFile file : ruleSetFiles()) {
            JsonNode root = parse(file);
            validate(schema, root, file.origin());
            ruleSets.add(toRuleSetFile(root, file.origin()));
        }
        // 로드 순서를 고정한다. 파일시스템 순회 순서에 결과가 의존하면 재현이 깨진다.
        ruleSets.sort(Comparator
                .comparing(RuleSetFile::productGroup)
                .thenComparingInt(RuleSetFile::version));
        return ruleSets;
    }

    private RuleSetFile toRuleSetFile(JsonNode root, String origin) {
        List<RuleFile> rules = new ArrayList<>();
        for (JsonNode node : root.withArray("rules")) {
            rules.add(new RuleFile(
                    node.get("code").asText(),
                    node.get("priority").asInt(),
                    node.path("description").asText(null),
                    writeJson(node.get("condition"), origin),
                    writeJson(node.get("effects"), origin)));
        }
        return new RuleSetFile(
                root.get("productGroup").asText(),
                root.get("version").asInt(),
                root.path("active").asBoolean(false),
                root.path("description").asText(null),
                List.copyOf(rules),
                origin);
    }

    // ── 가중치 ───────────────────────────────────────────────────

    @Override
    public List<DocumentWeightFile> loadDocumentWeights() {
        SourceFile file = weightsFile();
        if (file == null) {
            return List.of();
        }
        JsonNode root = parse(file);
        validate(loadSchema(WEIGHTS_SCHEMA), root, file.origin());

        List<DocumentWeightFile> weights = new ArrayList<>();
        for (JsonNode node : root.withArray("weights")) {
            weights.add(new DocumentWeightFile(
                    node.get("documentCode").asText(),
                    node.get("displayName").asText(),
                    node.get("requirement").asText(),
                    node.get("weight").asInt(),
                    node.path("note").asText(null)));
        }
        return List.copyOf(weights);
    }

    @Override
    public String describeSource() {
        return properties.hasExternalRulePath()
                ? "외부 경로 " + properties.path()
                : "jar 내장 rules/";
    }

    // ── 파일 탐색 ────────────────────────────────────────────────

    /** 파일 하나. {@code origin}은 오류 메시지에 쓰이는 사람이 읽을 수 있는 위치다. */
    private record SourceFile(String origin, ContentSupplier content) {
    }

    private interface ContentSupplier {
        InputStream open() throws IOException;
    }

    private List<SourceFile> ruleSetFiles() {
        if (properties.hasExternalRulePath()) {
            return externalYamlFiles(Path.of(properties.path()));
        }
        try {
            List<SourceFile> files = new ArrayList<>();
            for (Resource resource : resolver.getResources(RULE_CLASSPATH_PATTERN)) {
                files.add(new SourceFile(describe(resource), resource::getInputStream));
            }
            return files;
        } catch (IOException e) {
            throw new IllegalStateException("룰 파일을 찾을 수 없습니다: " + RULE_CLASSPATH_PATTERN, e);
        }
    }

    private SourceFile weightsFile() {
        if (properties.hasExternalWeightsPath()) {
            Path path = Path.of(properties.weightsPath()).resolve("document-weights.yaml");
            if (!Files.isReadable(path)) {
                throw new IllegalStateException("가중치 파일을 읽을 수 없습니다: " + path.toAbsolutePath());
            }
            return new SourceFile(path.toAbsolutePath().toString(), () -> Files.newInputStream(path));
        }
        Resource resource = resolver.getResource(WEIGHTS_CLASSPATH);
        if (!resource.exists()) {
            return null;
        }
        return new SourceFile(describe(resource), resource::getInputStream);
    }

    private List<SourceFile> externalYamlFiles(Path root) {
        if (!Files.isDirectory(root)) {
            throw new IllegalStateException(
                    "opencertflow.rules.path가 디렉터리가 아닙니다: " + root.toAbsolutePath());
        }
        try (Stream<Path> walk = Files.walk(root)) {
            return walk.filter(Files::isRegularFile)
                    .filter(p -> {
                        String name = p.getFileName().toString();
                        return name.endsWith(".yaml") || name.endsWith(".yml");
                    })
                    .sorted()
                    .map(p -> new SourceFile(
                            p.toAbsolutePath().toString(), () -> Files.newInputStream(p)))
                    .toList();
        } catch (IOException e) {
            throw new IllegalStateException("룰 디렉터리 순회 실패: " + root.toAbsolutePath(), e);
        }
    }

    private String describe(Resource resource) {
        try {
            return resource.getURI().toString();
        } catch (IOException e) {
            return resource.getDescription();
        }
    }

    // ── 파싱·검증 ────────────────────────────────────────────────

    private JsonNode parse(SourceFile file) {
        try (InputStream in = file.content().open()) {
            return yaml.readTree(in);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "룰 파일 YAML 파싱 실패 — %s: %s".formatted(file.origin(), e.getMessage()), e);
        }
    }

    private JsonSchema loadSchema(String classpathLocation) {
        try (InputStream in = YamlRuleFileAdapter.class.getResourceAsStream(classpathLocation)) {
            if (in == null) {
                throw new IllegalStateException("스키마를 찾을 수 없습니다: " + classpathLocation);
            }
            return JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012)
                    .getSchema(json.readTree(in));
        } catch (IOException e) {
            throw new IllegalStateException("스키마 로드 실패: " + classpathLocation, e);
        }
    }

    private void validate(JsonSchema schema, JsonNode document, String origin) {
        Set<ValidationMessage> errors = schema.validate(document);
        if (errors.isEmpty()) {
            return;
        }
        String detail = errors.stream()
                .map(ValidationMessage::getMessage)
                .sorted()
                .reduce("", (a, b) -> a.isEmpty() ? "  - " + b : a + "\n  - " + b);
        throw new IllegalStateException(
                "룰 파일이 스키마를 위반했습니다 — %s\n%s".formatted(origin, detail));
    }

    private String writeJson(JsonNode node, String origin) {
        try {
            return json.writeValueAsString(node);
        } catch (IOException e) {
            throw new IllegalStateException("룰 JSON 직렬화 실패 — " + origin, e);
        }
    }
}
