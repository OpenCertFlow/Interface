package com.certimakers;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.certimakers.common.application.annotation.UseCase;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.GeneralCodingRules;
import jakarta.persistence.Entity;
import java.time.Instant;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

/**
 * 헥사고날 아키텍처의 의존 규칙을 CI에서 강제한다.
 *
 * <p>Gradle 멀티모듈 대신 단일 모듈을 택한 대가로(ADR-0001), 컴파일러가 해 주던 일을 이 테스트가
 * 대신한다. <b>이 테스트가 CI에서 빠지면 그 결정의 전제가 무너진다.</b>
 *
 * <p>{@code common.config}는 규칙에서 제외한다. 조립을 담당하는 구성 루트(composition root)는
 * 모든 계층을 알아야 한다.
 */
@AnalyzeClasses(packages = "com.certimakers", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    private static final String DOMAIN = "..domain..";
    private static final String APPLICATION = "..application..";
    private static final String ADAPTER = "..adapter..";
    private static final String ADAPTER_IN = "..adapter.in..";
    private static final String ADAPTER_OUT = "..adapter.out..";
    private static final String CONFIG = "..common.config..";

    // ─────────────────────────────────────────────────────────────
    // 1. 의존성은 안쪽을 향한다
    // ─────────────────────────────────────────────────────────────

    @ArchTest
    static final ArchRule 도메인은_바깥_계층을_모른다 = noClasses()
            .that().resideInAPackage(DOMAIN)
            .should().dependOnClassesThat().resideInAnyPackage(APPLICATION, ADAPTER, CONFIG)
            .because("도메인은 자신이 어떻게 노출되고 저장되는지 몰라야 한다");

    @ArchTest
    static final ArchRule 애플리케이션은_어댑터를_모른다 = noClasses()
            .that().resideInAPackage(APPLICATION)
            .should().dependOnClassesThat().resideInAPackage(ADAPTER)
            .because("어댑터는 포트 인터페이스 뒤에 있어야 교체 가능하다");

    @ArchTest
    static final ArchRule 인바운드_어댑터는_아웃바운드_어댑터를_모른다 = noClasses()
            .that().resideInAPackage(ADAPTER_IN)
            .should().dependOnClassesThat().resideInAPackage(ADAPTER_OUT)
            .because("어댑터끼리는 서로를 모른다. 대화는 애플리케이션을 거친다");

    // ─────────────────────────────────────────────────────────────
    // 2. 도메인은 순수 자바다
    // ─────────────────────────────────────────────────────────────

    @ArchTest
    static final ArchRule 도메인은_프레임워크를_모른다 = noClasses()
            .that().resideInAPackage(DOMAIN)
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework..",
                    "jakarta.persistence..",
                    "jakarta.validation..",
                    "org.hibernate..",
                    "reactor..",
                    "com.fasterxml.jackson..")
            .because("도메인 테스트는 스프링 컨텍스트 없이 밀리초 안에 끝나야 한다");

    @ArchTest
    static final ArchRule 도메인은_시각과_난수를_직접_읽지_않는다 = noClasses()
            .that().resideInAPackage(DOMAIN)
            .should().callMethod(Instant.class, "now")
            .orShould().callMethod(UUID.class, "randomUUID")
            .orShould().callMethod(System.class, "currentTimeMillis")
            .because("TimeProvider·IdGenerator 포트를 거쳐야 룰 평가가 순수 함수로 유지된다");

    @ArchTest
    static final ArchRule JPA_엔티티는_영속성_어댑터에만_존재한다 = classes()
            .that().areAnnotatedWith(Entity.class)
            .should().resideInAPackage("..adapter.out.persistence..")
            .because("도메인 모델과 JPA 엔티티는 별개이며, 어댑터가 둘을 매핑한다");

    // ─────────────────────────────────────────────────────────────
    // 3. WebFlux + JPA 규율 (ADR-0002)
    // ─────────────────────────────────────────────────────────────

    @ArchTest
    static final ArchRule 애플리케이션에_트랜잭션_애노테이션을_붙이지_않는다 = noClasses()
            .that().resideInAPackage(APPLICATION)
            .should().beAnnotatedWith(Transactional.class)
            .because("리액티브 체인을 가로지르는 @Transactional은 동작하지 않는다. "
                    + "트랜잭션은 영속성 어댑터 안에서 시작하고 끝난다");

    @ArchTest
    static final ArchRule 애플리케이션_메서드에_트랜잭션_애노테이션을_붙이지_않는다 = noMethods()
            .that().areDeclaredInClassesThat().resideInAPackage(APPLICATION)
            .should().beAnnotatedWith(Transactional.class)
            .because("위와 같은 이유");

    // ─────────────────────────────────────────────────────────────
    // 4. 스테레오타입 일관성
    // ─────────────────────────────────────────────────────────────

    @ArchTest
    static final ArchRule UseCase는_애플리케이션_서비스에_있다 = classes()
            .that().areAnnotatedWith(UseCase.class)
            .should().resideInAPackage("..application.service..")
            .because("계층이 패키지 이름에 드러나야 한다");

    // ─────────────────────────────────────────────────────────────
    // 5. 일반 코딩 규칙
    // ─────────────────────────────────────────────────────────────

    @ArchTest
    static final ArchRule 필드_주입을_쓰지_않는다 = GeneralCodingRules.NO_CLASSES_SHOULD_USE_FIELD_INJECTION;

    @ArchTest
    static final ArchRule 표준_출력_스트림을_쓰지_않는다 = GeneralCodingRules.NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS;

    @ArchTest
    static final ArchRule java_util_logging을_쓰지_않는다 = GeneralCodingRules.NO_CLASSES_SHOULD_USE_JAVA_UTIL_LOGGING;

    @ArchTest
    static final ArchRule 바운디드_컨텍스트에_순환_의존이_없다 = slices()
            .matching("com.certimakers.(*)..")
            .should().beFreeOfCycles();
}
