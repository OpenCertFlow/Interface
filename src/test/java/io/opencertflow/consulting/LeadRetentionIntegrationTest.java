package io.opencertflow.consulting;

import static org.assertj.core.api.Assertions.assertThat;

import io.opencertflow.consulting.adapter.out.persistence.ConsultingLeadEntity;
import io.opencertflow.consulting.adapter.out.persistence.ConsultingLeadJpaRepository;
import io.opencertflow.consulting.application.port.in.PurgeExpiredLeadsUseCase;
import io.opencertflow.consulting.domain.model.LeadStatus;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * 개인정보 보존정책 파기(F-BE-014). 보존 기간(기본 180일)이 지난 <b>종착</b> 리드만 삭제하고,
 * 진행 중이거나 최근 리드는 남기는지 검증한다. 개인정보를 담은 리드가 목적 종료 후에도 무기한
 * 남지 않도록 하는 것이 이 기능의 목적이므로, "무엇을 지우지 않는가"가 특히 중요하다.
 */
@SpringBootTest
@ActiveProfiles("local")
@Testcontainers
class LeadRetentionIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    ConsultingLeadJpaRepository repository;

    @Autowired
    PurgeExpiredLeadsUseCase purgeExpiredLeadsUseCase;

    @Autowired
    JdbcTemplate jdbcTemplate;

    private static final long DIAGNOSIS_ID = 800_001L;

    private void seedDiagnosis() {
        jdbcTemplate.update(
                "INSERT INTO diagnosis (id, status, created_at, updated_at) "
                        + "VALUES (?, 'COMPLETED', now(), now()) ON CONFLICT (id) DO NOTHING",
                DIAGNOSIS_ID);
    }

    private void saveLead(long id, LeadStatus status, int ageDays) {
        Instant createdAt = Instant.now().minus(Duration.ofDays(ageDays));
        repository.save(new ConsultingLeadEntity(
                id, DIAGNOSIS_ID, "홍길동", "cipher-phone", "cipher-email",
                "문의", null, status.name(), createdAt));
    }

    @Test
    @DisplayName("보존 기간이 지난 완료·취소 리드만 파기하고 진행 중·최근 리드는 남긴다")
    void 보존_기간_경과_종착_리드만_파기한다() {
        seedDiagnosis();
        long oldCompleted = 800_101L;   // 200일 전 완료 → 파기 대상
        long oldCancelled = 800_102L;   // 200일 전 취소 → 파기 대상
        long oldInProgress = 800_103L;  // 200일 전이지만 진행 중 → 보존
        long recentCompleted = 800_104L; // 10일 전 완료 → 보존(기간 이내)
        saveLead(oldCompleted, LeadStatus.COMPLETED, 200);
        saveLead(oldCancelled, LeadStatus.CANCELLED, 200);
        saveLead(oldInProgress, LeadStatus.IN_PROGRESS, 200);
        saveLead(recentCompleted, LeadStatus.COMPLETED, 10);

        Long deleted = purgeExpiredLeadsUseCase.purgeExpired().block();

        assertThat(deleted).isEqualTo(2);
        assertThat(repository.existsById(oldCompleted)).isFalse();
        assertThat(repository.existsById(oldCancelled)).isFalse();
        assertThat(repository.existsById(oldInProgress)).isTrue();
        assertThat(repository.existsById(recentCompleted)).isTrue();
    }
}
