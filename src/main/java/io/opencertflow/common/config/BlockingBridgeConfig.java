package io.opencertflow.common.config;

import io.opencertflow.common.application.support.BlockingBridge;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

/**
 * 블로킹 격리 인프라를 조립한다. ADR-0002의 실행 지점.
 */
@Configuration
@EnableConfigurationProperties(BlockingProperties.class)
public class BlockingBridgeConfig {

    private static final Logger log = LoggerFactory.getLogger(BlockingBridgeConfig.class);

    /**
     * JPA/JDBC 전용 스케줄러. {@code boundedElastic} 기본 인스턴스를 쓰지 않는 이유는, 그것이
     * 파일 I/O 등 다른 블로킹 작업과 공유되어 커넥션 풀 크기와 스레드 수를 맞출 수 없기 때문이다.
     */
    @Bean(destroyMethod = "dispose")
    public Scheduler jdbcScheduler(BlockingProperties properties) {
        return Schedulers.newBoundedElastic(
                properties.jdbcPoolSize(),
                properties.queueCapacity(),
                "jdbc",
                properties.ttlSeconds(),
                true);
    }

    @Bean
    public BlockingBridge blockingBridge(Scheduler jdbcScheduler) {
        return new BlockingBridge(jdbcScheduler);
    }

    /**
     * 스케줄러 스레드 수와 커넥션 풀 크기가 어긋나면 경고한다.
     *
     * <p>실패시키지 않고 경고만 하는 이유는, 로컬/테스트 환경에서 의도적으로 다르게 둘 수 있기
     * 때문이다. 운영 배포 전에 로그를 확인하는 것이 체크리스트 항목이다.
     */
    @Bean
    public ApplicationRunner blockingPoolSizeConsistencyCheck(
            BlockingProperties properties, ObjectProvider<DataSource> dataSourceProvider) {
        return args -> {
            DataSource dataSource = dataSourceProvider.getIfAvailable();
            if (!(dataSource instanceof HikariDataSource hikari)) {
                return;
            }
            int hikariMax = hikari.getMaximumPoolSize();
            if (hikariMax != properties.jdbcPoolSize()) {
                log.warn(
                        "jdbcScheduler 스레드 수({})와 HikariCP maximum-pool-size({})가 다릅니다. "
                                + "두 값은 같아야 합니다. opencertflow.blocking.jdbc-pool-size 또는 "
                                + "spring.datasource.hikari.maximum-pool-size를 확인하세요.",
                        properties.jdbcPoolSize(),
                        hikariMax);
            } else {
                log.info("블로킹 격리 준비 완료 — jdbcScheduler 스레드 {}개, HikariCP 커넥션 {}개",
                        properties.jdbcPoolSize(), hikariMax);
            }
        };
    }
}
