package com.certimakers.auth.adapter.out.persistence;

import com.certimakers.auth.application.port.out.LoadUserPort;
import com.certimakers.auth.application.port.out.SaveUserPort;
import com.certimakers.auth.domain.model.AuthProvider;
import com.certimakers.auth.domain.model.Email;
import com.certimakers.auth.domain.model.User;
import com.certimakers.auth.domain.model.UserId;
import com.certimakers.common.adapter.out.persistence.annotation.PersistenceAdapter;
import java.util.Optional;
import org.springframework.transaction.annotation.Transactional;

/** {@link SaveUserPort}·{@link LoadUserPort}의 JPA 구현. 메서드는 블로킹이다. */
@PersistenceAdapter
public class UserPersistenceAdapter implements SaveUserPort, LoadUserPort {

    private final UserJpaRepository repository;

    public UserPersistenceAdapter(UserJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public User save(User user) {
        repository.save(UserMapper.toEntity(user));
        return user;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findById(UserId id) {
        return repository.findById(id.value()).map(UserMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByEmail(Email email) {
        return repository.findByEmail(email.value()).map(UserMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByKakaoId(String kakaoId) {
        return repository.findByProviderAndProviderId(AuthProvider.KAKAO.name(), kakaoId)
                .map(UserMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByEmail(Email email) {
        return repository.existsByEmail(email.value());
    }
}
