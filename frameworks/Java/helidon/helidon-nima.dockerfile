FROM docker.io/maven:3.9.6-eclipse-temurin-21 as maven

WORKDIR /helidon
COPY nima/src src
COPY nima/pom.xml pom.xml
RUN mvn package -q


FROM openjdk:21-jdk-slim
WORKDIR /helidon
COPY --from=maven /helidon/target/libs libs
COPY --from=maven /helidon/target/benchmark-nima.jar app.jar

ENV no_proxy 127.0.0.1,localhost,localhost4,localhost6,*.localdomain,*.localdomain4,*.localdomain6,localaddress,tfb-server,tfb-database
ENV NO_PROXY 127.0.0.1,localhost,localhost4,localhost6,*.localdomain,*.localdomain4,*.localdomain6,localaddress,tfb-server,tfb-database

EXPOSE 8080

CMD java -XX:+UseNUMA \
    -XX:+UseParallelGC \
    -jar app.jar
