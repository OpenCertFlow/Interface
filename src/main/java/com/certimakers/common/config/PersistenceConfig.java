package com.certimakers.common.config;

import com.certimakers.common.domain.port.TimeProvider;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing(dateTimeProviderRef = "auditingDateTimeProvider")
public class PersistenceConfig {

    /**
     * 저장 시각은 항상 UTC로 기록한다. 표시 시각대는 클라이언트가 결정한다.
     * 서버가 KST로 저장하기 시작하면 배포 리전이 바뀌는 날 데이터가 어긋난다.
     */
    @Bean
    public Clock clock() {
        return Clock.system(ZoneOffset.UTC);
    }

    /**
     * JPA 감사(auditing)도 {@link TimeProvider}를 거치게 한다. 테스트에서 {@code Clock}을 고정하면
     * 엔티티의 {@code createdAt}까지 함께 고정된다.
     */
    @Bean
    public DateTimeProvider auditingDateTimeProvider(TimeProvider timeProvider) {
        return () -> Optional.of(timeProvider.now());
    }
}
