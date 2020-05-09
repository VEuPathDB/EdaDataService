# # # # # # # # # # # # # # # # # # # # # # # # # # # # # #
#
#   Build Service & Dependencies
#
# # # # # # # # # # # # # # # # # # # # # # # # # # # # # #
FROM foxcapades/alpine-oracle:1.3 AS prep

LABEL service="demo-service"

ENV DOCKER=build\
    JAVA_HOME=/opt/jdk \
    PATH=/mvn/bin:/opt/jdk/bin:$PATH

WORKDIR /workspace
RUN wget https://download.java.net/java/early_access/alpine/10/binaries/openjdk-15-ea+10_linux-x64-musl_bin.tar.gz \
    && tar -xzf openjdk-15-ea+10_linux-x64-musl_bin.tar.gz \
    && rm openjdk-15-ea+10_linux-x64-musl_bin.tar.gz \
    && jdk-15/bin/jlink \
       --compress=2 \
       --module-path jdk-15/jmods \
       --add-modules java.base,java.logging,java.xml,java.desktop,java.management \
       --output /jlinked \
    && mv jdk-15 /opt/jdk \
    && apk add --no-cache git sed findutils coreutils make npm\
    && wget https://mirrors.gigenet.com/apache/maven/maven-3/3.6.3/binaries/apache-maven-3.6.3-bin.tar.gz \
    && tar -xzf apache-maven-3.6.3-bin.tar.gz \
    && rm apache-maven-3.6.3-bin.tar.gz \
    && mv apache-maven-3.6.3 /mvn \
    && git config --global advice.detachedHead false

COPY . .
RUN mkdir -p vendor \
    && cp -n /jdbc/* vendor \
    && make jar

# # # # # # # # # # # # # # # # # # # # # # # # # # # # # #
#
#   Run the service
#
# # # # # # # # # # # # # # # # # # # # # # # # # # # # # #
FROM foxcapades/alpine-oracle:1.3

LABEL service="demo-service"

ENV JAVA_HOME=/opt/jdk \
    PATH=/opt/jdk/bin:$PATH

COPY --from=prep /jlinked /opt/jdk
COPY --from=prep /workspace/build/libs/service.jar /service.jar

EXPOSE 8080

CMD java -jar /service.jar

