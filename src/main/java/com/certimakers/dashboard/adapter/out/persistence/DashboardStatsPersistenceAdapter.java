package com.certimakers.dashboard.adapter.out.persistence;

import com.certimakers.audit.adapter.out.persistence.AuditLogJpaRepository;
import com.certimakers.auth.adapter.out.persistence.UserJpaRepository;
import com.certimakers.common.adapter.out.persistence.annotation.PersistenceAdapter;
import com.certimakers.consulting.adapter.out.persistence.ConsultingLeadJpaRepository;
import com.certimakers.dashboard.application.port.out.DashboardStatsPort;
import com.certimakers.diagnosis.adapter.out.persistence.diagnosis.DiagnosisJpaRepository;
import com.certimakers.diagnosis.adapter.out.persistence.document.OfficialDocumentJpaRepository;
import com.certimakers.diagnosis.adapter.out.persistence.rule.RuleSetJpaRepository;
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
