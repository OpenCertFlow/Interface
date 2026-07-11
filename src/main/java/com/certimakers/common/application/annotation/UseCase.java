package com.certimakers.common.application.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.stereotype.Component;

/**
 * 인바운드 포트(UseCase)를 구현하는 애플리케이션 서비스.
 *
 * <p>{@code @Service} 대신 이 애노테이션을 쓰는 이유는 계층이 이름에 드러나야 하기 때문이다.
 * ArchUnit이 {@code ..application.service..} 밖의 {@code @UseCase} 사용을 막는다.
 *
 * <p><b>주의</b>: 여기에 {@code @Transactional}을 붙이지 말 것. WebFlux 리액티브 체인에서는
 * 동작하지 않는다. 트랜잭션은 영속성 어댑터 내부에서 시작하고 끝난다(ADR-0002 참조).
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface UseCase {

    String value() default "";
}
