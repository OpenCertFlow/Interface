package com.certimakers.common.domain.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 애그리거트 루트. 일관성 경계의 유일한 진입점이며, 한 트랜잭션에서 통째로 저장·조회된다.
 *
 * <p>동일성은 <b>식별자</b>로만 판단한다. 값이 전부 같아도 id가 다르면 다른 애그리거트다.
 * 이것이 값 객체(record로 구현, 값 기반 동일성)와의 결정적 차이다.
 *
 * @param <ID> 식별자 타입
 */
public abstract class AggregateRoot<ID> {

    private final transient List<DomainEvent> domainEvents = new ArrayList<>();

    public abstract ID id();

    protected void registerEvent(DomainEvent event) {
        domainEvents.add(Objects.requireNonNull(event, "event"));
    }

    /**
     * 등록된 이벤트를 꺼내고 비운다. 영속성 어댑터가 저장 직후 한 번 호출한다.
     *
     * <p>"꺼내면서 비운다"는 계약이 중요하다. 같은 이벤트를 두 번 발행하지 않기 위함이다.
     */
    public List<DomainEvent> pullDomainEvents() {
        List<DomainEvent> pulled = List.copyOf(domainEvents);
        domainEvents.clear();
        return pulled;
    }

    public List<DomainEvent> domainEvents() {
        return Collections.unmodifiableList(domainEvents);
    }

    @Override
    public final boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        ID thisId = id();
        return thisId != null && thisId.equals(((AggregateRoot<?>) other).id());
    }

    @Override
    public final int hashCode() {
        return Objects.hashCode(id());
    }
}
