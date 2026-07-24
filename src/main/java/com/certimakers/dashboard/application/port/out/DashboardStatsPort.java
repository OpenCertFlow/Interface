package com.certimakers.dashboard.application.port.out;

/** 관리자 대시보드 집계. 블로킹(JPA count)이라 호출자는 BlockingBridge로 감싼다. */
public interface DashboardStatsPort {

    Stats load();

    record Stats(long userCount, long consultantCount, long adminCount, long diagnosisCount,
                 long consultingLeadCount, long ruleSetCount, long activeRuleSetCount,
                 long officialDocumentCount, long auditLogCount) {
    }
}
