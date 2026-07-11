package com.certimakers.common.adapter.out.external.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.stereotype.Component;

/**
 * 아웃바운드 포트를 외부 HTTP 호출(AI 워커, LLM)로 구현하는 어댑터.
 *
 * <p>이 어댑터의 메서드는 <b>논블로킹</b>({@code Mono}/{@code Flux})이며, 반드시 타임아웃을
 * 명시한다. 실패는 {@code ExternalSystemException}으로 변환하여 호출자가 폴백을 결정하게 한다.
 * 어댑터가 스스로 폴백 값을 지어내면 안 된다 — 그것은 애플리케이션의 정책 결정이다.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface ExternalAdapter {

    String value() default "";
}
