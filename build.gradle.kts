import org.veupathdb.lib.gradle.container.util.Logger.Level
import java.io.FileOutputStream
import java.net.URL

plugins {
  kotlin("jvm") version "1.7.0" // needed for local compute import
  java
  id("org.veupathdb.lib.gradle.container.container-utils") version "4.8.9"
  id("com.github.johnrengelman.shadow") version "7.1.2"
}

// configure VEupathDB container plugin
containerBuild {

  // Change if debugging the build process is necessary.
  logLevel = Level.Info

  // General project level configuration.
  project {

    // Project Name
    name = "eda-data-service"

    // Project Group
    group = "org.veupathdb.service.eda"

    // Project Version
    version = "3.0.0"

    // Project Root Package
    projectPackage = "org.veupathdb.service.eda"

    // Main Class Name
    mainClassName = "ds.Main"
  }

  // Docker build configuration.
  docker {

    // Docker build context
    context = "."

    // Name of the target docker file
    dockerFile = "Dockerfile"

    // Resulting image tag
    imageName = "eda-data"

  }
}

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(17))
  }
}

tasks.shadowJar {
  exclude("**/Log4j2Plugins.dat")
  archiveFileName.set("service.jar")
}

repositories {
  mavenCentral()
  mavenLocal()
  maven {
    name = "GitHubPackages"
    url  = uri("https://maven.pkg.github.com/veupathdb/maven-packages")
    credentials {
      username = if (extra.has("gpr.user")) extra["gpr.user"] as String? else System.getenv("GITHUB_USERNAME")
      password = if (extra.has("gpr.key")) extra["gpr.key"] as String? else System.getenv("GITHUB_TOKEN")
    }
  }
}

//
// Project Dependencies
//

// versions
val coreLib       = "6.19.1"         // Container core lib version
val edaCompute    = "2.4.4"          // EDA Compute version (used to pull in compute plugin RAML)
val edaCommon     = "11.6.7"         // EDA Common version
val fgputil       = "2.12.9-jakarta" // FgpUtil version

// use local EDA compute compiled schema if project exists, else use released version;
//    this mirrors the way we use local EdaCommon code if available
val commonRamlOutFileName = "$projectDir/schema/eda-compute-lib.raml"

tasks.named("merge-raml") {
  // Hook into merge-raml to download or fetch EDA Common RAML before merging
  doFirst {
    val commonRamlOutFile = File(commonRamlOutFileName)
    commonRamlOutFile.delete()

    // use local EdaCommon compiled schema if project exists, else use released version;
    // this mirrors the way we use local EdaCommon code if available
    val edaComputeProject = file("../service-eda-compute")
    if (edaComputeProject.exists()) {
      val commonRamlFile = File("../service-eda-compute/schema/library.raml")
      logger.lifecycle("Copying file from ${commonRamlFile.path} to ${commonRamlOutFile.path}")
      commonRamlFile.copyTo(commonRamlOutFile)
    } else {
      commonRamlOutFile.createNewFile()
      val commonRamlUrl = "https://raw.githubusercontent.com/VEuPathDB/service-eda-compute/v${edaCompute}/schema/library.raml"
      logger.lifecycle("Downloading file contents from $commonRamlUrl")
      URL(commonRamlUrl).openStream().use { it.transferTo(FileOutputStream(commonRamlOutFile)) }
    }
  }

  // After merge is complete, delete the EDA Common RAML from this project.
  doLast {
    logger.lifecycle("Deleting file $commonRamlOutFileName")
    File(commonRamlOutFileName).delete()
  }
}

// ensures changing modules are never cached
configurations.all {
  resolutionStrategy.cacheChangingModulesFor(0, TimeUnit.SECONDS)
}

dependencies {

  // REngine Java client to RServe
  //implementation("org.rosuda.REngine:REngine:2.1.0")
  implementation("org.rosuda.REngine:Rserve:1.8.1")

  // VEuPathDB libs, prefer local checkouts if available
  implementation(findProject(":core") ?: "org.veupathdb.lib:jaxrs-container-core:${coreLib}")
  implementation(findProject(":edaCommon") ?: "org.veupathdb.service.eda:eda-common:${edaCommon}")

  // published VEuPathDB libs
  implementation("org.gusdb:fgputil-core:${fgputil}")
  implementation("org.gusdb:fgputil-client:${fgputil}")
  implementation("org.gusdb:fgputil-db:${fgputil}")

  // Jersey
  implementation("org.glassfish.jersey.core:jersey-server:3.1.1")

  // Jackson
  implementation("com.fasterxml.jackson.core:jackson-databind:2.15.1")
  implementation("com.fasterxml.jackson.core:jackson-annotations:2.15.1")

  // Log4J
  implementation("org.apache.logging.log4j:log4j-api:2.20.0")
  implementation("org.apache.logging.log4j:log4j-core:2.20.0")

  // Metrics
  implementation("io.prometheus:simpleclient:0.16.0")
  implementation("io.prometheus:simpleclient_common:0.16.0")

  // Utils
  implementation("io.vulpine.lib:Jackfish:1.1.0")
  implementation("com.devskiller.friendly-id:friendly-id:1.1.0")

  // Unit Testing
  testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.2")
  testImplementation("org.mockito:mockito-core:5.2.0")
  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.2")
}

val test by tasks.getting(Test::class) {
  // Use junit platform for unit tests
  useJUnitPlatform()
}
