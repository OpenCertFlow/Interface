# OpenCertFlow 백엔드 배포 이미지(F-BE-019). 멀티스테이지로 빌드 도구를 런타임에서 분리한다.

# ── 1단계: 빌드 ──────────────────────────────────────────────────────────────
# gradlew 래퍼를 그대로 써서 로컬과 같은 Gradle 버전으로 빌드한다.
FROM eclipse-temurin:17-jdk AS build
WORKDIR /workspace

# 의존성 캐시 레이어: 빌드 스크립트만 먼저 복사해 받아 두면, 소스만 바뀔 때 재다운로드를 피한다.
COPY gradlew gradlew.bat settings.gradle build.gradle ./
COPY gradle gradle
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies > /dev/null 2>&1 || true

COPY src src
# 이미지 빌드 시 테스트는 CI에서 이미 돈다 — 여기서는 부트 실행 가능한 jar만 만든다.
RUN ./gradlew --no-daemon clean bootJar -x test

# ── 2단계: 런타임 ────────────────────────────────────────────────────────────
# JDK가 아닌 JRE만 담아 이미지 크기·공격면을 줄이고, 비루트 사용자로 실행한다.
FROM eclipse-temurin:17-jre AS runtime
WORKDIR /app

RUN groupadd --system app && useradd --system --gid app app
COPY --from=build /workspace/build/libs/*-SNAPSHOT.jar app.jar
USER app

EXPOSE 8080
# 컨테이너 메모리 한도에 맞춰 힙을 잡도록 위임한다(고정 -Xmx 대신).
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
