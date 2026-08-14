FROM eclipse-temurin@sha256:ad0cdd9782db550ca7dde6939a16fd850d04e683d37d3cff79d84a5848ba6a5a

RUN apk add --no-cache tzdata && \
    addgroup -S appgroup && \
    adduser -S appuser -G appgroup

WORKDIR /app

RUN mkdir -p /app/logs && chown -R appuser:appgroup /app

COPY extracted/dependencies/ ./
COPY extracted/application/ ./

ENV TZ=Africa/Nairobi

EXPOSE 8086

USER appuser

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-XX:+UseG1GC", \
  "-XX:+ExitOnOutOfMemoryError"]

CMD ["-jar", "notifications-0.0.1-SNAPSHOT.jar"]

ARG BUILD_DATE
ARG VCS_REF
ARG VERSION

LABEL org.opencontainers.image.created="${BUILD_DATE}"
LABEL org.opencontainers.image.description=""
LABEL org.opencontainers.image.revision="${VCS_REF}"
LABEL org.opencontainers.image.source="https://github.com/EbikesAfrica254/notifications"
LABEL org.opencontainers.image.title="Ebikes Africa notifications service"
LABEL org.opencontainers.image.version="${VERSION}"
