FROM docker.io/maven:3.9.2-eclipse-temurin-20 as maven

RUN wget https://download.java.net/java/GA/jdk21/fd2272bbf8e04c3dbaee13770090416c/35/GPL/openjdk-21_linux-x64_bin.tar.gz
RUN tar zxvf openjdk-21_linux-x64_bin.tar.gz
ENV JAVA_HOME $HOME/jdk-21
ENV PATH $JAVA_HOME/bin:$PATH

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

CMD java --enable-preview \
    -XX:+UseNUMA \
    -XX:+UseParallelGC \
    -jar app.jar
