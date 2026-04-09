# eclipse-temurin 21-jre-alpine as of 2026-04-09
FROM eclipse-temurin@sha256:2ad3e67ed80e421b8a2e0e733805dcc29ae2fcd2c0f5b4d1d9dad3c4ac86e45c

RUN apk add --no-cache shadow tzdata

WORKDIR /app

RUN addgroup -S appgroup && \
    adduser -S appuser -G appgroup && \
    mkdir -p /app/logs && \
    chown -R appuser:appgroup /app

COPY --chown=appuser:appgroup dependencies/ ./
COPY --chown=appuser:appgroup spring-boot-loader/ ./
COPY --chown=appuser:appgroup snapshot-dependencies*/ ./
COPY --chown=appuser:appgroup application/ ./

ENV TZ=Africa/Nairobi

EXPOSE 8086

USER appuser

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0"]
CMD ["org.springframework.boot.loader.launch.JarLauncher"]

ARG VERSION
ARG BUILD_DATE
ARG VCS_REF

LABEL org.opencontainers.image.title="Ebikes Africa notifications service"
LABEL org.opencontainers.image.description=""
LABEL org.opencontainers.image.version="${VERSION}"
LABEL org.opencontainers.image.created="${BUILD_DATE}"
LABEL org.opencontainers.image.source="https://github.com/EbikesAfrica254/notifications"
LABEL org.opencontainers.image.revision="${VCS_REF}"