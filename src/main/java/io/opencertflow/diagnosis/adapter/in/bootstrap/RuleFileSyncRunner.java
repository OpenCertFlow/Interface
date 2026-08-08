package io.opencertflow.diagnosis.adapter.in.bootstrap;

import io.opencertflow.diagnosis.application.port.in.SyncRuleFilesUseCase;
import io.opencertflow.diagnosis.application.port.in.SyncRuleFilesUseCase.SyncResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 기동 시 룰·가중치 파일을 저장소에 반영하는 인바운드 어댑터.
 *
 * <p>예전에는 이 일을 Flyway 반복 마이그레이션({@code R__seed_rules.sql})이 했다. 파일로 옮긴
 * 이유는 SQL {@code INSERT} 문자열 안에 박힌 JSON은 커뮤니티가 리뷰할 수 없기 때문이다.
 * 이제 룰 변경은 {@code rules/}의 YAML diff로 드러난다.
 *
 * <p>기동 스레드에서 실행되므로 이벤트 루프와 무관하다. 요청을 받기 전에 끝난다.
 *
 * <p><b>실패하면 기동을 중단시킨다.</b> 룰이 없는 서버는 모든 진단에 "적용 규칙을 찾지 못했다"고
 * 답한다. 그것은 조용한 오작동이라, 뜨지 않는 편이 낫다.
 *
 * <p>동기화를 끄려면 {@code opencertflow.rules.sync-enabled=false}. 그러면 이 빈 자체가 만들어지지
 * 않는다 — 인바운드 어댑터가 설정 값을 읽어 스스로 판단하면 아웃바운드 설정 클래스에 의존하게
 * 되고, 그것은 ArchUnit이 막는 방향이다.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@ConditionalOnProperty(
        prefix = "opencertflow.rules",
        name = "sync-enabled",
        havingValue = "true",
        matchIfMissing = true)
public class RuleFileSyncRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(RuleFileSyncRunner.class);

    private final SyncRuleFilesUseCase syncRuleFiles;

    public RuleFileSyncRunner(SyncRuleFilesUseCase syncRuleFiles) {
        this.syncRuleFiles = syncRuleFiles;
    }

    @Override
    public void run(ApplicationArguments args) {
        SyncResult result = syncRuleFiles.sync();

        if (result.isEmpty()) {
            log.warn("반영된 룰이 없습니다. rules/ 디렉터리가 클래스패스에 있는지 확인하세요. "
                    + "룰이 없으면 모든 진단이 '적용 규칙 없음'으로 나옵니다. (출처: {})", result.source());
            return;
        }
        log.info("룰 파일 동기화 완료 — 룰셋 {}개 / 룰 {}개 / 가중치 신규 {}개 (출처: {})",
                result.ruleSets(), result.rules(), result.weightsInserted(), result.source());
    }
}
