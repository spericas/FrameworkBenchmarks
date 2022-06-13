FROM maven:3.8.1-openjdk-17-slim as maven

ENV http_proxy http://www-proxy-hqdc.us.oracle.com:80
ENV https_proxy http://www-proxy-hqdc.us.oracle.com:80
ENV HTTP_PROXY http://www-proxy-hqdc.us.oracle.com:80
ENV HTTPS_PROXY http://www-proxy-hqdc.us.oracle.com:80
ENV MAVEN_OPTS="-Dhttp.proxyHost=www-proxy-hqdc.us.oracle.com -Dhttp.proxyPort=80 -Dhttps.proxyHost=www-proxy-hqdc.us.oracle.com -Dhttps.proxyPort=80"

ENV no_proxy 127.0.0.1,localhost,localhost4,localhost6,*.localdomain,*.localdomain4,*.localdomain6,localaddress,tfb-server,tfb-database
ENV NO_PROXY 127.0.0.1,localhost,localhost4,localhost6,*.localdomain,*.localdomain4,*.localdomain6,localaddress,tfb-server,tfb-database

RUN apt-get -yqq update
RUN apt-get -yqq install git-core
RUN git clone https://github.com/oracle/helidon.git helidon-master
WORKDIR /helidon-master
RUN mvn clean install -DskipTests -q
WORKDIR /helidon
COPY src src
COPY pom.xml pom.xml
RUN mvn package -q

FROM openjdk:17.0.1-jdk-slim
WORKDIR /helidon
COPY --from=maven /helidon/target/libs libs
COPY --from=maven /helidon/target/benchmark.jar app.jar

EXPOSE 8080

CMD java -server \
    -XX:-UseBiasedLocking \
    -XX:+UseNUMA \
    -XX:+UseParallelGC \
    -Dio.netty.buffer.checkBounds=false \
    -Dio.netty.buffer.checkAccessible=false \
    -jar app.jar
