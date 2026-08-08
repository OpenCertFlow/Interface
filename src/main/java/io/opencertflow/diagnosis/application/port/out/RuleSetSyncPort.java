package io.opencertflow.diagnosis.application.port.out;

import io.opencertflow.diagnosis.application.port.out.RuleFileSourcePort.DocumentWeightFile;
import io.opencertflow.diagnosis.application.port.out.RuleFileSourcePort.RuleSetFile;
import java.util.List;

/**
 * 파일에서 읽은 정의를 런타임 저장소에 반영한다. 블로킹(JPA)이며 기동 스레드에서만 호출된다.
 */
public interface RuleSetSyncPort {

    /**
     * 같은 (제품군, 버전)의 룰셋을 파일 내용으로 <b>교체</b>한다. 기존 룰은 지워진다.
     *
     * <p>병합이 아니라 교체인 이유: 파일이 진실의 원천이므로, 파일에서 지운 룰이 DB에 남아 계속
     * 발동하면 {@code git diff}가 실제 동작을 설명하지 못하게 된다.
     *
     * @return 반영된 룰 개수
     */
    int replaceRuleSet(RuleSetFile ruleSet);

    /**
     * 없는 가중치만 채운다. 이미 있는 행은 건드리지 않는다.
     *
     * <p>교체가 아니라 삽입인 이유: 관리자 API로 조정한 가중치가 재기동으로 되돌아가면 안 된다.
     * 파일은 "기본값"이고, 운영 중 권위는 관리 API가 갖는다.
     *
     * @return 새로 삽입된 개수
     */
    int insertMissingWeights(List<DocumentWeightFile> weights);
}
