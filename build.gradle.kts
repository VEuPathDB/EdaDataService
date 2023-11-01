import org.veupathdb.lib.gradle.container.util.Logger.Level

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
    mainClassName = "Main"
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

kotlin {
  jvmToolchain {
    languageVersion.set(JavaLanguageVersion.of(17))
  }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
  kotlinOptions {
    jvmTarget = "17"
    freeCompilerArgs = listOf(
      "-Xjvm-default=all"
    )
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
val coreLib       = "6.16.0"          // Container core lib version
val edaCompute    = "2.2.0"           // EDA Compute version (used to pull in compute plugin RAML)
val fgputil       = "2.12.12-jakarta-SNAPSHOT" // FgpUtil version
val libSubsetting = "4.11.1"           // lib-eda-subsetting version

// use local EDA compute compiled schema if project exists, else use released version;
//    this mirrors the way we use local EdaCommon code if available
val commonRamlOutFileName = "$projectDir/schema/eda-compute-lib.raml"

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
  implementation(findProject(":libSubsetting") ?: "org.veupathdb.eda:lib-eda-subsetting:${libSubsetting}")
  implementation("org.veupathdb.lib:compute-platform:1.5.3")

  // published VEuPathDB libs
  implementation("org.gusdb:fgputil-core:${fgputil}")
  implementation("org.gusdb:fgputil-accountdb:${fgputil}")
  implementation("org.gusdb:fgputil-client:${fgputil}")
  implementation("org.gusdb:fgputil-db:${fgputil}")

  // Jersey
  implementation("org.glassfish.jersey.core:jersey-server:3.1.1")

  // Jackson
  implementation("com.fasterxml.jackson.core:jackson-databind:2.15.1")
  implementation("com.fasterxml.jackson.core:jackson-annotations:2.15.1")
  implementation("org.veupathdb.lib:jackson-singleton:3.0.0")
  implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.14.2")

  // Log4J
  implementation("org.apache.logging.log4j:log4j-api:2.20.0")
  implementation("org.apache.logging.log4j:log4j-core:2.20.0")
  runtimeOnly("org.apache.logging.log4j:log4j-slf4j-impl:2.20.0")
  implementation("org.slf4j:slf4j-api:1.7.36")

  // Metrics
  implementation("io.prometheus:simpleclient:0.16.0")
  implementation("io.prometheus:simpleclient_common:0.16.0")

  // Utils
  implementation("io.vulpine.lib:Jackfish:1.1.0")
  implementation("com.devskiller.friendly-id:friendly-id:1.1.0")
  implementation("io.vulpine.lib:sql-import:0.2.1")
  implementation("io.vulpine.lib:lib-query-util:2.1.0")
  implementation("javax.mail", "mail", "1.5.0-b01")
  implementation("org.antlr", "ST4", "4.3.1") // Access service email template parsing

  // Pico CLI
  implementation("info.picocli:picocli:4.7.3")
  annotationProcessor("info.picocli:picocli-codegen:4.7.3")

  // Job IDs
  implementation("org.veupathdb.lib:hash-id:1.1.0")

  // Stub database
  implementation("org.hsqldb:hsqldb:2.7.1")

  // Unit Testing
  testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.2")
  testImplementation("org.mockito:mockito-core:5.2.0")
  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.2")
}

val test by tasks.getting(Test::class) {
  // Use junit platform for unit tests
  useJUnitPlatform()
}
