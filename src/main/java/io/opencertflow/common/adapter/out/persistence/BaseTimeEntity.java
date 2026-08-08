package io.opencertflow.common.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import java.time.Instant;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * 생성·수정 시각을 자동으로 채우는 JPA 매핑 상위 클래스.
 *
 * <p>이 클래스는 <b>어댑터 계층에만</b> 존재한다. 도메인 애그리거트가 이것을 상속하면
 * 도메인이 JPA에 의존하게 되고 ArchUnit이 실패한다. 도메인 모델과 JPA 엔티티는 별개이며,
 * 영속성 어댑터가 둘 사이를 매핑한다(ADR-0001).
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseTimeEntity {

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
