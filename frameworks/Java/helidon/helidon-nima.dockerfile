FROM docker.io/maven:3.8.6-eclipse-temurin-19 as maven

ENV http_proxy http://www-proxy-hqdc.us.oracle.com:80
ENV https_proxy http://www-proxy-hqdc.us.oracle.com:80
ENV HTTP_PROXY http://www-proxy-hqdc.us.oracle.com:80
ENV HTTPS_PROXY http://www-proxy-hqdc.us.oracle.com:80
ENV MAVEN_OPTS="-Dhttp.proxyHost=www-proxy-hqdc.us.oracle.com -Dhttp.proxyPort=80 -Dhttps.proxyHost=www-proxy-hqdc.us.oracle.com -Dhttps.proxyPort=80"
ENV no_proxy 127.0.0.1,localhost,localhost4,localhost6,*.localdomain,*.localdomain4,*.localdomain6,localaddress,tfb-server,tfb-database
ENV NO_PROXY 127.0.0.1,localhost,localhost4,localhost6,*.localdomain,*.localdomain4,*.localdomain6,localaddress,tfb-server,tfb-database

WORKDIR /helidon
COPY nima/src src
COPY nima/pom.xml pom.xml
RUN mvn package -q

FROM openjdk:19-jdk-slim
WORKDIR /helidon
COPY --from=maven /helidon/target/libs libs
COPY --from=maven /helidon/target/benchmark-nima.jar app.jar

EXPOSE 8080

CMD java --enable-preview \
    -jar app.jar
