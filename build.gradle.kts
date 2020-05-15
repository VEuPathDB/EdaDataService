import java.util.Properties
import java.io.FileInputStream

plugins {
  java
}

apply(from = "dependencies.gradle.kts")

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
