package io.opencertflow.dashboard.adapter.out.persistence;

import io.opencertflow.audit.adapter.out.persistence.AuditLogJpaRepository;
import io.opencertflow.auth.adapter.out.persistence.UserJpaRepository;
import io.opencertflow.common.adapter.out.persistence.annotation.PersistenceAdapter;
import io.opencertflow.consulting.adapter.out.persistence.ConsultingLeadJpaRepository;
import io.opencertflow.dashboard.application.port.out.DashboardStatsPort;
import io.opencertflow.diagnosis.adapter.out.persistence.diagnosis.DiagnosisJpaRepository;
import io.opencertflow.diagnosis.adapter.out.persistence.document.OfficialDocumentJpaRepository;
import io.opencertflow.diagnosis.adapter.out.persistence.rule.RuleSetJpaRepository;
import org.springframework.transaction.annotation.Transactional;

/**
 * 대시보드 집계. 각 컨텍스트의 리포지토리 count를 모은다 — 읽기 전용 요약이라 컨텍스트 포트를 일일이
 * 두지 않고 리포지토리를 직접 센다(대시보드 → 각 컨텍스트 단방향, 순환 없음).
 */
@PersistenceAdapter
public class DashboardStatsPersistenceAdapter implements DashboardStatsPort {

    private final UserJpaRepository userRepository;
    private final DiagnosisJpaRepository diagnosisRepository;
    private final ConsultingLeadJpaRepository consultingLeadRepository;
    private final RuleSetJpaRepository ruleSetRepository;
    private final OfficialDocumentJpaRepository officialDocumentRepository;
    private final AuditLogJpaRepository auditLogRepository;

    public DashboardStatsPersistenceAdapter(
            UserJpaRepository userRepository,
            DiagnosisJpaRepository diagnosisRepository,
            ConsultingLeadJpaRepository consultingLeadRepository,
            RuleSetJpaRepository ruleSetRepository,
            OfficialDocumentJpaRepository officialDocumentRepository,
            AuditLogJpaRepository auditLogRepository) {
        this.userRepository = userRepository;
        this.diagnosisRepository = diagnosisRepository;
        this.consultingLeadRepository = consultingLeadRepository;
        this.ruleSetRepository = ruleSetRepository;
        this.officialDocumentRepository = officialDocumentRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Stats load() {
        return new Stats(
                userRepository.countByRole("USER"),
                userRepository.countByRole("CONSULTANT"),
                userRepository.countByRole("ADMIN"),
                diagnosisRepository.count(),
                consultingLeadRepository.count(),
                ruleSetRepository.count(),
                ruleSetRepository.countByActiveIsTrue(),
                officialDocumentRepository.count(),
                auditLogRepository.count());
    }
}
