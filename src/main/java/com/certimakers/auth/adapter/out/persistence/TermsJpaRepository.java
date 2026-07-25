package com.certimakers.auth.adapter.out.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TermsJpaRepository extends JpaRepository<TermsEntity, Long> {

    List<TermsEntity> findByActiveIsTrueOrderByRequiredDescTermKeyAsc();
}
