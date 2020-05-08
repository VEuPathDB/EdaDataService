#
# Build a minimized JDK
#
FROM alpine:latest AS jdk

WORKDIR /opt/jdk
RUN wget https://download.java.net/java/early_access/alpine/10/binaries/openjdk-15-ea+10_linux-x64-musl_bin.tar.gz \
    && tar -xzf openjdk-15-ea+10_linux-x64-musl_bin.tar.gz \
    && rm openjdk-15-ea+10_linux-x64-musl_bin.tar.gz \
    && jdk-15/bin/jlink \
         --compress=2 \
         --module-path jdk-15/jmods \
         --add-modules java.base \
         --output /jlinked

#
# Build the RAML -> JaxRS generator
#
FROM alpine:latest AS maven

COPY --from=jdk /opt/jdk/* /opt/jdk/

ENV MAVEN_VERSION=3.6.3 \
    JAVA_HOME=/opt/jdk

WORKDIR /tmp/mvn/
RUN apk add --no-cache git sed
RUN wget https://mirrors.gigenet.com/apache/maven/maven-3/$MAVEN_VERSION/binaries/apache-maven-$MAVEN_VERSION-bin.tar.gz \
    && tar -xzf apache-maven-$MAVEN_VERSION-bin.tar.gz \
    && rm apache-maven-$MAVEN_VERSION-bin.tar.gz \
    && mv apache-maven-$MAVEN_VERSION /mvn

ENV PATH=/mvn/bin:$PATH

WORKDIR /tmp/build
COPY bin/build-raml2jaxrs.sh bin/install-fgputil.sh ./
RUN ./build-raml2jaxrs.sh && ./install-fgputil.sh docker

#
# Build the service itself
#
FROM alpine:latest AS service

COPY --from=jdk /opt/jdk/* /opt/jdk/
COPY --from=maven /tmp/build/raml-to-jaxrs.jar /workspace/
COPY --from=maven /tmp/build/fgputil-util-1.0.0.jar /workspace/vendor/

ENV JAVA_HOME=/opt/jdk \
    PATH=/opt/jdk/bin:$PATH

RUN apk add jq findutils make npm grep
WORKDIR /workspace

COPY bin/prepare-env.sh bin/schema2raml.sh ./bin/
RUN bin/prepare-env.sh docker

COPY gradle/ ./gradle
COPY gradlew ./
COPY makefile .
COPY service.properties build.gradle.kts ./
COPY docs/schema ./docs/schema
COPY api.raml .
COPY src/ ./src

RUN make build-jar

#
# Run the service
#
FROM alpine:latest

ENV JAVA_HOME=/opt/jdk \
    PATH=/opt/jdk/bin:$PATH
COPY --from=jdk /jlinked /opt/jdk
COPY --from=service /workspace/build/libs/service.jar /service.jar

CMD java -jar /service.jar

