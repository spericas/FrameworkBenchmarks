#
# Copyright (c) 2018, 2019 Oracle and/or its affiliates. All rights reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# 1st stage, build the app
FROM maven:3.6.1-jdk-11-slim as build

WORKDIR /helidonmp

# Create a first layer to cache the "Maven World" in the local repository.
# Incremental docker builds will always resume after that, unless you update
# the pom
ADD microprofile/pom.xml .
RUN mvn -Dhttp.proxyHost=www-proxy-hqdc.us.oracle.com -Dhttp.proxyPort=80 -Dhttps.proxyHost=www-proxy-hqdc.us.oracle.com -Dhttps.proxyPort=80 package -q

# Do the Maven build!
# Incremental docker builds will resume here when you change sources
ADD microprofile/src src
RUN mvn -Dhttp.proxyHost=www-proxy-hqdc.us.oracle.com -Dhttp.proxyPort=80 -Dhttps.proxyHost=www-proxy-hqdc.us.oracle.com -Dhttps.proxyPort=80 package -q

# 2nd stage, build the runtime image
FROM openjdk:15-slim
WORKDIR /helidonmp

# Copy the binary built in the 1st stage
COPY --from=build /helidonmp/target/benchmark-mp.jar ./
COPY --from=build /helidonmp/target/libs ./libs

CMD ["java", "-server", "-XX:+UseNUMA", "-XX:+UseParallelGC", "-jar", "benchmark-mp.jar"]

EXPOSE 8080
