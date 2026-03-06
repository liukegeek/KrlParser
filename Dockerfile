# syntax=docker/dockerfile:1.7

FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace

# 先复制 POM，尽量复用依赖缓存。
COPY pom.xml ./
COPY krl-core/pom.xml krl-core/pom.xml
COPY krl-web/pom.xml krl-web/pom.xml
RUN mvn -B -pl krl-web -am -DskipTests dependency:go-offline

# 再复制源码并执行打包。
COPY krl-core/src krl-core/src
COPY krl-web/src krl-web/src
RUN mvn -B -pl krl-web -am -DskipTests clean package \
    && find /workspace/krl-web/target -maxdepth 1 -type f -name 'krl-web-*.jar' ! -name '*.original' -exec cp {} /workspace/krl-web-app.jar \;

FROM eclipse-temurin:21-jre
WORKDIR /app

RUN groupadd --system krl \
    && useradd --system --gid krl --home-dir /app/data --create-home krl \
    && mkdir -p /app/data/logs /app/data/tmp /app/data/results \
    && chown -R krl:krl /app

COPY --from=build /workspace/krl-web-app.jar /app/krl-parser.jar

USER krl
EXPOSE 2026
VOLUME ["/app/data"]

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-Dfile.encoding=UTF-8", "-jar", "/app/krl-parser.jar"]
