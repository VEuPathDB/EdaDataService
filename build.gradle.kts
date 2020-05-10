import java.util.Properties
import java.io.FileInputStream

plugins {
  java
}

// Load Props
val buildProps = Properties()
buildProps.load(FileInputStream(File(rootDir, "service.properties")))
val fullPack = "${buildProps["app.package.root"]}.${buildProps["app.package.service"]}"

// Project settings
group = buildProps["project.group"] ?: error("empty 1")
version = buildProps["project.version"] ?: error("empty 2")

repositories {
  jcenter()
}

dependencies {

  //
  // FgpUtil & Compatibility Dependencies
  //

  // FgpUtil jars
  implementation(files(
    "vendor/fgputil-util-1.0.0.jar",
    "vendor/fgputil-accountdb-1.0.0.jar"
  ))

  // Compatibility bridge to support the long dead log4j-1.X
  runtimeOnly("org.apache.logging.log4j:log4j-1.2-api:${buildProps["version.log4j"]}")
  runtimeOnly("org.apache.commons:commons-dbcp2:2.7.0")

  //
  // Project Dependencies
  //

  // Oracle
  runtimeOnly(files(
    "vendor/ojdbc8.jar",
    "vendor/ucp.jar",
    "vendor/xstreams.jar"
  ))

  // JavaX
  implementation("javax.ws.rs:javax.ws.rs-api:2.1.1")

  // Jersey
  implementation("org.glassfish.jersey.containers:jersey-container-grizzly2-http:${buildProps["version.jersey"]}")
  implementation("org.glassfish.jersey.containers:jersey-container-grizzly2-servlet:${buildProps["version.jersey"]}")
  implementation("org.glassfish.jersey.media:jersey-media-json-jackson:${buildProps["version.jersey"]}")
  runtimeOnly("org.glassfish.jersey.inject:jersey-hk2:${buildProps["version.jersey"]}")

  // Jackson
  implementation("com.fasterxml.jackson.core:jackson-databind:${buildProps["version.jackson"]}")
  implementation("com.fasterxml.jackson.core:jackson-annotations:${buildProps["version.jackson"]}")
  implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:${buildProps["version.jackson"]}")

  // CLI
  implementation("info.picocli:picocli:4.2.0")
  annotationProcessor("info.picocli:picocli-codegen:4.2.0")


  // Log4J
  implementation("org.apache.logging.log4j:log4j-api:${buildProps["version.log4j"]}")
  implementation("org.apache.logging.log4j:log4j-core:${buildProps["version.log4j"]}")
  implementation("org.apache.logging.log4j:log4j:${buildProps["version.log4j"]}")

  // Utils
  implementation("io.vulpine.lib:Jackfish:1.1.0")

  // Unit Testing
  testImplementation("org.junit.jupiter:junit-jupiter-api:${buildProps["version.junit"]}")
  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${buildProps["version.junit"]}")
}

tasks.jar {
  manifest {
    attributes["Main-Class"] = "${fullPack}.${buildProps["app.main-class"]}"
    attributes["Implementation-Title"] = buildProps["project.name"]
    attributes["Implementation-Version"] = buildProps["project.version"]
  }
  from(configurations.runtimeClasspath.get().map {
    if (it.isDirectory) it else zipTree(it)
  })
  archiveFileName.set("service.jar")
  exclude("log4j.properties", "log4j.xml")
}

tasks.register("print-package") { print(fullPack) }
tasks.register("print-container-name") { print(buildProps["container.name"]) }

val test by tasks.getting(Test::class) {
  // Use junit platform for unit tests
  useJUnitPlatform()
}
