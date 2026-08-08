package io.opencertflow.auth.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** {@code app_user} 스프링 데이터 리포지토리. */
public interface UserJpaRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByEmail(String email);

    Optional<UserEntity> findByProviderAndProviderId(String provider, String providerId);

    boolean existsByEmail(String email);

    List<UserEntity> findByOrderByCreatedAtDesc(Pageable pageable);

    List<UserEntity> findByRoleOrderByCreatedAtDesc(String role, Pageable pageable);

    long countByRole(String role);
}
