# # # # # # # # # # # # # # # # # # # # # # # # # # # # # #
#
#   Build Service & Dependencies
#
# # # # # # # # # # # # # # # # # # # # # # # # # # # # # #
FROM veupathdb/alpine-dev-base:jdk-18 AS prep

LABEL service="eda-data-build"

ARG GITHUB_USERNAME
ARG GITHUB_TOKEN

WORKDIR /workspace

RUN jlink --compress=2 --module-path /opt/jdk/jmods \
       --add-modules java.base,java.net.http,java.security.jgss,java.logging,java.xml,java.desktop,java.management,jdk.management,java.sql,java.naming \
       --output /jlinked \
    && apk add --no-cache git sed findutils coreutils make npm curl gawk jq \
    && git config --global advice.detachedHead false

RUN npm install -gs raml2html raml2html-modern-theme

# download gradle
COPY gradlew ./
COPY gradle gradle
RUN bash -c 'echo "\n\n" | ./gradlew init --type basic --dsl kotlin --no-daemon'

# copy files required to build dev environment and fetch dependencies
COPY build.gradle.kts settings.gradle.kts ./

# download raml tools (these never change)
RUN ./gradlew install-raml-4-jax-rs install-raml-merge

# download project dependencies in advance
RUN ./gradlew download-dependencies

# copy raml over for merging, then perform code and documentation generation
COPY api.raml ./
COPY schema schema
RUN ./gradlew generate-jaxrs generate-raml-docs

# copy remaining files
COPY . .

# build the project
RUN ./gradlew clean test shadowJar


# # # # # # # # # # # # # # # # # # # # # # # # # # # # # #
#
#   Run the service
#
# # # # # # # # # # # # # # # # # # # # # # # # # # # # # #
FROM alpine:3.16

LABEL service="eda-data"

RUN apk add --no-cache tzdata \
    && cp /usr/share/zoneinfo/America/New_York /etc/localtime \
    && echo "America/New_York" > /etc/timezone

ENV JAVA_HOME=/opt/jdk \
    PATH=/opt/jdk/bin:$PATH \
    JVM_MEM_ARGS="" \
    JVM_ARGS=""

COPY --from=prep /jlinked /opt/jdk
COPY --from=prep /workspace/build/libs/service.jar /service.jar

CMD java -jar -XX:+CrashOnOutOfMemoryError $JVM_MEM_ARGS $JVM_ARGS /service.jar
