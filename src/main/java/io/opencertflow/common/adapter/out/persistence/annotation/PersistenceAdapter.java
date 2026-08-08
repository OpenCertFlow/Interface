package io.opencertflow.common.adapter.out.persistence.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.stereotype.Component;

/**
 * 아웃바운드 포트를 JPA로 구현하는 영속성 어댑터.
 *
 * <p>이 어댑터의 메서드는 <b>블로킹</b>이다. 호출자(애플리케이션 서비스)는 반드시
 * {@code BlockingBridge}를 통해 호출해야 한다. 트랜잭션 경계는 이 클래스 안에서 시작하고 끝난다.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface PersistenceAdapter {

    String value() default "";
}
