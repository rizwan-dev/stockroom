# Build and runtime are separate stages so the shipped image carries a JRE and a
# jar, not a JDK, Maven and the whole dependency cache.
#
# The -jammy tags are deliberate: eclipse-temurin's Alpine variants at 17 are
# published for amd64 only, so an -alpine base fails to build on Apple Silicon
# and on arm64 CI runners.
FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /build

# Copy only what resolves dependencies first. This layer is cached and skipped
# on every build where pom.xml has not changed, which is almost all of them.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline

COPY src/ src/
RUN ./mvnw -B -q -DskipTests package

FROM eclipse-temurin:17-jre-jammy AS runtime
WORKDIR /app

# curl is only here for the healthcheck; --no-install-recommends keeps it to
# the one package rather than pulling in a documentation tree with it.
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

# Not root. A container that does not need to write to its own filesystem
# should not be able to.
RUN useradd --system --create-home --shell /usr/sbin/nologin app
USER app

COPY --from=build /build/target/*.jar app.jar

EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=3s --start-period=60s --retries=10 \
  CMD curl -fsS http://localhost:8080/actuator/health/readiness || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
