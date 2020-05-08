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
  implementation(files("vendor/fgputil-util-1.0.0.jar"))
  implementation("javax.ws.rs:javax.ws.rs-api:2.1.1")

  implementation("org.glassfish.jersey.containers:jersey-container-grizzly2-http:${buildProps["version.jersey"]}")
  implementation("org.glassfish.jersey.media:jersey-media-json-jackson:${buildProps["version.jersey"]}")
  runtimeOnly("org.glassfish.jersey.inject:jersey-hk2:${buildProps["version.jersey"]}")

  implementation("com.fasterxml.jackson.core:jackson-databind:${buildProps["version.jackson"]}")
  implementation("com.fasterxml.jackson.core:jackson-annotations:${buildProps["version.jackson"]}")

  implementation("org.slf4j:slf4j-api:1.7.30")
  implementation("org.apache.logging.log4j:log4j-slf4j-impl:2.13.2")

  testImplementation("org.junit.jupiter:junit-jupiter-api:${buildProps["version.junit"]}")
  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${buildProps["version.junit"]}")
}


tasks {
  jar {
    manifest {
      attributes["Main-Class"] = "${fullPack}.${buildProps["app.main-class"]}"
    }
    from(configurations.runtimeClasspath.get().map {if (it.isDirectory) it else zipTree(it) })
    archiveFileName.set("service.jar")
  }
}

tasks.register("print-package") { print(fullPack) }
tasks.register("print-container-name") { print(buildProps["container.name"]) }

val test by tasks.getting(Test::class) {
  // Use junit platform for unit tests
  useJUnitPlatform()
}

