package io.opencertflow.diagnosis.adapter.out.persistence.rule;

import io.opencertflow.common.domain.port.IdGenerator;
import io.opencertflow.common.domain.port.TimeProvider;
import io.opencertflow.diagnosis.application.port.out.RuleFileSourcePort.DocumentWeightFile;
import io.opencertflow.diagnosis.application.port.out.RuleFileSourcePort.RuleFile;
import io.opencertflow.diagnosis.application.port.out.RuleFileSourcePort.RuleSetFile;
import io.opencertflow.diagnosis.application.port.out.RuleSetSyncPort;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 파일에서 읽은 룰셋·가중치를 테이블에 반영한다.
 *
 * <p>트랜잭션은 여기서 시작하고 끝난다(ADR-0002). 룰셋 교체는 삭제와 삽입이 한 단위여야 한다 —
 * 중간에 실패해 룰이 지워진 채로 남으면 그 제품군의 진단이 통째로 503이 된다.
 */
@Component
public class RuleSetSyncPersistenceAdapter implements RuleSetSyncPort {

    private final RuleSetJpaRepository ruleSetRepository;
    private final DocumentWeightJpaRepository weightRepository;
    private final IdGenerator idGenerator;
    private final TimeProvider timeProvider;

    public RuleSetSyncPersistenceAdapter(
            RuleSetJpaRepository ruleSetRepository,
            DocumentWeightJpaRepository weightRepository,
            IdGenerator idGenerator,
            TimeProvider timeProvider) {
        this.ruleSetRepository = ruleSetRepository;
        this.weightRepository = weightRepository;
        this.idGenerator = idGenerator;
        this.timeProvider = timeProvider;
    }

    @Override
    @Transactional
    public int replaceRuleSet(RuleSetFile file) {
        // 같은 (제품군, 버전)의 기존 룰셋을 지운다. rule은 FK CASCADE로 함께 사라진다.
        // 활성 룰셋은 제품군당 하나만 허용되는 부분 유니크 인덱스가 걸려 있으므로, 새 활성본을
        // 넣기 전에 같은 제품군의 다른 활성본도 내려야 한다.
        ruleSetRepository.findAllByOrderByProductGroupAscVersionDesc().stream()
                .filter(existing -> existing.getProductGroup().equals(file.productGroup()))
                .filter(existing -> existing.getVersion() == file.version())
                .forEach(ruleSetRepository::delete);
        ruleSetRepository.flush();

        if (file.active()) {
            deactivateOthers(file.productGroup());
        }

        RuleSetEntity entity =
                new RuleSetEntity(idGenerator.nextId(), file.version(), file.productGroup());
        for (RuleFile rule : file.rules()) {
            entity.addRule(new RuleEntity(
                    idGenerator.nextId(),
                    rule.code(),
                    rule.priority(),
                    rule.conditionJson(),
                    rule.effectsJson(),
                    rule.description()));
        }
        if (file.active()) {
            entity.activate(timeProvider.now());
        }
        ruleSetRepository.save(entity);
        return file.rules().size();
    }

    /** 같은 제품군의 다른 활성 룰셋을 내린다. 부분 유니크 인덱스 충돌을 피하기 위함이다. */
    private void deactivateOthers(String productGroup) {
        Optional<RuleSetEntity> active =
                ruleSetRepository.findByProductGroupAndActiveIsTrue(productGroup);
        active.ifPresent(existing -> {
            existing.deactivate();
            ruleSetRepository.saveAndFlush(existing);
        });
    }

    @Override
    @Transactional
    public int insertMissingWeights(List<DocumentWeightFile> weights) {
        int inserted = 0;
        for (DocumentWeightFile weight : weights) {
            if (weightRepository.existsById(weight.documentCode())) {
                continue; // 관리자가 조정한 값을 재기동으로 되돌리지 않는다
            }
            weightRepository.save(new DocumentWeightEntity(
                    weight.documentCode(),
                    weight.displayName(),
                    weight.requirement(),
                    weight.weight(),
                    weight.note()));
            inserted++;
        }
        return inserted;
    }
}
