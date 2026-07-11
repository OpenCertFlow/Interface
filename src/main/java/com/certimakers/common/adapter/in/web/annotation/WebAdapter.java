package com.certimakers.common.adapter.in.web.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.web.bind.annotation.RestController;

/**
 * 인바운드 포트를 HTTP로 노출하는 웹 어댑터.
 *
 * <p>책임은 셋뿐이다: 요청 DTO 검증, 커맨드로 변환, 응답 DTO로 변환. 비즈니스 판단은 하지 않는다.
 * 웹 어댑터가 아웃바운드 어댑터를 참조하면 ArchUnit이 실패한다.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@RestController
public @interface WebAdapter {

    String value() default "";
}
