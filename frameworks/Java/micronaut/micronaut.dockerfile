FROM gradle:6.9-jdk11 as build

ENV http_proxy http://www-proxy-hqdc.us.oracle.com:80
ENV https_proxy http://www-proxy-hqdc.us.oracle.com:80
ENV HTTP_PROXY http://www-proxy-hqdc.us.oracle.com:80
ENV HTTPS_PROXY http://www-proxy-hqdc.us.oracle.com:80
ENV MAVEN_OPTS="-Dhttp.proxyHost=www-proxy-hqdc.us.oracle.com -Dhttp.proxyPort=80 -Dhttps.proxyHost=www-proxy-hqdc.us.oracle.com -Dhttps.proxyPort=80"

ENV no_proxy 127.0.0.1,localhost,localhost4,localhost6,*.localdomain,*.localdomain4,*.localdomain6,localaddress,tfb-server,tfb-database
ENV NO_PROXY 127.0.0.1,localhost,localhost4,localhost6,*.localdomain,*.localdomain4,*.localdomain6,localaddress,tfb-server,tfb-database

WORKDIR /micronaut
COPY src src
COPY build.gradle build.gradle
COPY settings.gradle settings.gradle
RUN gradle -Dhttp.proxyHost=www-proxy-hqdc.us.oracle.com -Dhttp.proxyPort=80 -Dhttps.proxyHost=www-proxy-hqdc.us.oracle.com -Dhttps.proxyPort=80 build buildLayers --no-daemon

FROM openjdk:11-jre-slim
WORKDIR /micronaut
COPY --from=build /micronaut/build/docker/layers/libs /home/app/libs
COPY --from=build /micronaut/build/docker/layers/resources /home/app/resources
COPY --from=build /micronaut/build/docker/layers/application.jar /home/app/application.jar

EXPOSE 8080

CMD ["java", "-server", "-XX:+UseNUMA", "-XX:+UseParallelGC", "-Dmicronaut.environments=benchmark", "-Dlog-root-level=OFF", "-jar", "/home/app/application.jar"]
