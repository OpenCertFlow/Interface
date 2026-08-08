package io.opencertflow.diagnosis.application.service;

import io.opencertflow.common.application.annotation.UseCase;
import io.opencertflow.diagnosis.application.port.in.SyncRuleFilesUseCase;
import io.opencertflow.diagnosis.application.port.out.RuleFileSourcePort;
import io.opencertflow.diagnosis.application.port.out.RuleFileSourcePort.DocumentWeightFile;
import io.opencertflow.diagnosis.application.port.out.RuleFileSourcePort.RuleSetFile;
import io.opencertflow.diagnosis.application.port.out.RuleSetSyncPort;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 파일 → 런타임 저장소 동기화.
 *
 * <p><b>리액티브가 아니다.</b> 기동 시 딱 한 번, 요청을 받기 전에, 기동 스레드에서 실행된다.
 * 이벤트 루프가 관여하지 않으므로 {@code BlockingBridge}가 필요 없다.
 *
 * <p>실패하면 기동을 중단시킨다. 룰이 없거나 깨진 채로 뜬 서버는 "인증이 필요 없습니다"라고
 * 조용히 틀린 답을 하기 때문이다 — 안 뜨는 편이 낫다.
 */
@UseCase
public class RuleFileSyncService implements SyncRuleFilesUseCase {

    private static final Logger log = LoggerFactory.getLogger(RuleFileSyncService.class);

    private final RuleFileSourcePort ruleFileSource;
    private final RuleSetSyncPort ruleSetSync;

    public RuleFileSyncService(RuleFileSourcePort ruleFileSource, RuleSetSyncPort ruleSetSync) {
        this.ruleFileSource = ruleFileSource;
        this.ruleSetSync = ruleSetSync;
    }

    @Override
    public SyncResult sync() {
        List<RuleSetFile> ruleSets = ruleFileSource.loadRuleSets();
        int rules = 0;
        for (RuleSetFile ruleSet : ruleSets) {
            rules += ruleSetSync.replaceRuleSet(ruleSet);
            log.debug("룰셋 반영 — {} v{} ({}개 룰, {})",
                    ruleSet.productGroup(), ruleSet.version(), ruleSet.rules().size(), ruleSet.origin());
        }

        List<DocumentWeightFile> weights = ruleFileSource.loadDocumentWeights();
        int inserted = ruleSetSync.insertMissingWeights(weights);

        return new SyncResult(ruleSets.size(), rules, inserted, ruleFileSource.describeSource());
    }
}
