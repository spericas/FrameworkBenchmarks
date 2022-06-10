FROM maven:3.8.1-openjdk-17-slim as maven
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