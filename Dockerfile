FROM eclipse-temurin:17-jre-jammy

RUN groupadd --system app && useradd --system --gid app --home-dir /app app

WORKDIR /app

COPY target/finsight-aggregator.jar app.jar

RUN chown -R app:app /app

USER app

EXPOSE 8080

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]