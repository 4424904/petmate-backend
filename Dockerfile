# Multi-stage build for Spring Boot
FROM eclipse-temurin:21-jdk-alpine as build

# 작업 디렉토리 설정
WORKDIR /app

# Gradle wrapper와 build.gradle 복사
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# 의존성 다운로드 (캐시 효율성)
RUN chmod +x ./gradlew
RUN ./gradlew dependencies --no-daemon

# 소스 코드 복사
COPY src src

# 애플리케이션 빌드
RUN ./gradlew bootJar --no-daemon

# 실행 단계
FROM eclipse-temurin:21-jre-alpine

# 애플리케이션 JAR 복사
COPY --from=build /app/build/libs/*.jar app.jar

# 포트 노출
EXPOSE 8090

# 애플리케이션 실행
ENTRYPOINT ["java", "-jar", "/app.jar"]