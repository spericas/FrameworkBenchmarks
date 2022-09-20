FROM maven:3.6.1-jdk-11-slim as maven

ENV http_proxy http://www-proxy-hqdc.us.oracle.com:80
ENV https_proxy http://www-proxy-hqdc.us.oracle.com:80
ENV HTTP_PROXY http://www-proxy-hqdc.us.oracle.com:80
ENV HTTPS_PROXY http://www-proxy-hqdc.us.oracle.com:80
ENV MAVEN_OPTS="-Dhttp.proxyHost=www-proxy-hqdc.us.oracle.com -Dhttp.proxyPort=80 -Dhttps.proxyHost=www-proxy-hqdc.us.oracle.com -Dhttps.proxyPort=80"

ENV no_proxy 127.0.0.1,localhost,localhost4,localhost6,*.localdomain,*.localdomain4,*.localdomain6,localaddress,tfb-server,tfb-datab
ENV NO_PROXY 127.0.0.1,localhost,localhost4,localhost6,*.localdomain,*.localdomain4,*.localdomain6,localaddress,tfb-server,tfb-datab

WORKDIR /netty
COPY pom.xml pom.xml
COPY src src
RUN mvn compile assembly:single -q

FROM openjdk:11.0.3-jdk-slim
WORKDIR /netty
COPY --from=maven /netty/target/netty-example-0.1-jar-with-dependencies.jar app.jar

EXPOSE 8080

CMD ["java", "-server", "-XX:+UseNUMA", "-XX:+UseParallelGC", "-XX:+AggressiveOpts", "-jar", "app.jar"]
