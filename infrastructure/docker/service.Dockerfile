# ---------------------------------------------------------------------------
# One Dockerfile for all eight JVM applications.
#
# The gateway and the seven services differ only in which module is built and
# which jar is copied, so they are parameterised with build args rather than
# duplicated into eight near-identical files that would then drift apart. Each
# still produces its own image; only the recipe is shared.
# ---------------------------------------------------------------------------

# ----------------------------------------------------------------- build ---
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /build

# Copy the POMs first and resolve dependencies on their own layer. Source
# changes then reuse the cached dependency layer instead of re-downloading the
# world on every build.
COPY pom.xml ./
COPY shared/common-lib/pom.xml shared/common-lib/
COPY api-gateway/pom.xml api-gateway/
COPY services/auth-service/pom.xml services/auth-service/
COPY services/product-service/pom.xml services/product-service/
COPY services/cart-service/pom.xml services/cart-service/
COPY services/order-service/pom.xml services/order-service/
COPY services/payment-service/pom.xml services/payment-service/
COPY services/inventory-service/pom.xml services/inventory-service/
COPY services/notification-service/pom.xml services/notification-service/

ARG MODULE
RUN mvn -B -pl ${MODULE} -am dependency:go-offline -DskipTests

COPY shared shared
COPY api-gateway api-gateway
COPY services services

# `-am` also builds common-lib, which every service needs.
RUN mvn -B -pl ${MODULE} -am package -DskipTests

# --------------------------------------------------------------- runtime ---
# A JRE, not a JDK: the compiler, javadoc tool and debugger are all build-time
# concerns and every one of them is extra attack surface in a running container.
FROM eclipse-temurin:21-jre-alpine AS runtime

# Never run as root. A container escape from an unprivileged process is a much
# smaller problem than one from uid 0.
RUN addgroup -S novamart && adduser -S novamart -G novamart

WORKDIR /app

ARG MODULE
ARG ARTIFACT
COPY --from=build /build/${MODULE}/target/${ARTIFACT}.jar app.jar
RUN chown novamart:novamart /app/app.jar

USER novamart

# MaxRAMPercentage rather than a fixed -Xmx, so the heap follows whatever the
# container is actually given instead of being tuned for one deployment and
# silently wrong everywhere else.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=70 -XX:+UseSerialGC -XX:TieredStopAtLevel=1"

EXPOSE 8080

# Compose gates dependent services on this, so a service that has started but is
# not yet ready to serve does not cause a cascade of connection failures.
HEALTHCHECK --interval=15s --timeout=3s --start-period=45s --retries=5 \
    CMD wget -qO- http://localhost:${SERVER_PORT:-8080}/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
