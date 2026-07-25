package com.certimakers.auth.adapter.out.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TermsJpaRepository extends JpaRepository<TermsEntity, UUID> {

    List<TermsEntity> findByActiveIsTrueOrderByRequiredDescTermKeyAsc();
}
