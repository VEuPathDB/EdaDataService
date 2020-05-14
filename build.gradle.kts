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

  // Extra FgpUtil dependencies
  runtimeOnly("org.apache.commons:commons-dbcp2:2.+")
  runtimeOnly("org.json:json:20190722")
  runtimeOnly("com.fasterxml.jackson.datatype:jackson-datatype-json-org:${buildProps["version.jackson"]}")
  runtimeOnly("com.fasterxml.jackson.module:jackson-module-parameter-names:${buildProps["version.jackson"]}")
  runtimeOnly("com.fasterxml.jackson.datatype:jackson-datatype-jdk8:${buildProps["version.jackson"]}")
  runtimeOnly("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:${buildProps["version.jackson"]}")

  //
  // Project Dependencies
  //

  // Oracle
  runtimeOnly(files(
    "vendor/ojdbc8.jar",
    "vendor/ucp.jar",
    "vendor/xstreams.jar"
  ))


  implementation(findProject(":core") ?: "org.veupathdb.lib:jaxrs-container-core:1.0.8")


  // Jersey
  implementation("org.glassfish.jersey.containers:jersey-container-grizzly2-http:${buildProps["version.jersey"]}")
  implementation("org.glassfish.jersey.containers:jersey-container-grizzly2-servlet:${buildProps["version.jersey"]}")
  implementation("org.glassfish.jersey.media:jersey-media-json-jackson:${buildProps["version.jersey"]}")
  runtimeOnly("org.glassfish.jersey.inject:jersey-hk2:${buildProps["version.jersey"]}")

  // Jackson
  implementation("com.fasterxml.jackson.core:jackson-databind:${buildProps["version.jackson"]}")
  implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:${buildProps["version.jackson"]}")

  // CLI
  implementation("info.picocli:picocli:4.+")
  annotationProcessor("info.picocli:picocli-codegen:4.+")

  // Log4J
  implementation("org.apache.logging.log4j:log4j-api:${buildProps["version.log4j"]}")
  implementation("org.apache.logging.log4j:log4j-core:${buildProps["version.log4j"]}")
  implementation("org.apache.logging.log4j:log4j:${buildProps["version.log4j"]}")

  // Metrics
  implementation("io.prometheus:simpleclient:0.9.0")
  implementation("io.prometheus:simpleclient_common:0.9.0")

  // Utils
  implementation("io.vulpine.lib:Jackfish:1.+")
  implementation("com.devskiller.friendly-id:friendly-id:1.+")

  // Unit Testing
  testImplementation("org.junit.jupiter:junit-jupiter-api:${buildProps["version.junit"]}")
  testImplementation("org.mockito:mockito-core:2.+")
  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${buildProps["version.junit"]}")
}

tasks.jar {
  manifest {
    attributes["Main-Class"] = "${fullPack}.${buildProps["app.main-class"]}"
    attributes["Implementation-Title"] = buildProps["project.name"]
    attributes["Implementation-Version"] = buildProps["project.version"]
  }
  println("Packaging Components")
  from(configurations.runtimeClasspath.get().map {
    println("  " + it.name)

    if (it.isDirectory) it else zipTree(it).matching {
      exclude { f -> f.name.toLowerCase().contains("log4j") &&
        f.name.toLowerCase().contains(".dat") } } })
  archiveFileName.set("service.jar")
}

tasks.register("print-package") { print(fullPack) }
tasks.register("print-container-name") { print(buildProps["container.name"]) }

val test by tasks.getting(Test::class) {
  // Use junit platform for unit tests
  useJUnitPlatform()
}
