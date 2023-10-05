import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
  kotlin("jvm") version "1.7.0" // needed for local compute import
  java
}

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(17))
  }
}


dependencies {
  implementation(kotlin("stdlib-jdk8"))

  testImplementation("com.fasterxml.jackson.core:jackson-core:2.15.1")
  testImplementation("com.fasterxml.jackson.core:jackson-databind:2.15.1")
  testImplementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.15.1")
  testImplementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.15.1")
  testImplementation("org.veupathdb.service.eda:eda-common:10.9.0")

  testImplementation("org.awaitility:awaitility:4.2.0")
  testImplementation("org.junit.jupiter:junit-jupiter:5.9.2")
  testImplementation("io.rest-assured:rest-assured:5.3.0")
  testImplementation("io.rest-assured:json-path:5.3.0")
  testImplementation("org.slf4j:slf4j-api:2.0.5")
  testImplementation("org.apache.logging.log4j:log4j-api-kotlin:1.2.0")
  testImplementation("org.apache.logging.log4j:log4j-core:2.20.0")
}


tasks.register<Test>("api-test") {
  systemProperty("AUTH_TOKEN", System.getenv("AUTH_TOKEN"))
  systemProperty("BASE_URL", System.getenv("EDA_BASE_URL"))
  systemProperty("SERVICE_PORT", System.getenv("EDA_SERVICE_PORT"))

  useJUnitPlatform()
  testLogging {
    events = setOf(TestLogEvent.STANDARD_OUT, TestLogEvent.STARTED, TestLogEvent.PASSED, TestLogEvent.SKIPPED, TestLogEvent.FAILED)
    showStackTraces = true
    showStandardStreams = true
  }
}

repositories {
  mavenCentral()
  maven {
    name = "GitHubPackages"
    url  = uri("https://maven.pkg.github.com/veupathdb/maven-packages")
    credentials {
      username = if (extra.has("gpr.user")) extra["gpr.user"] as String? else System.getenv("GITHUB_USERNAME")
      password = if (extra.has("gpr.key")) extra["gpr.key"] as String? else System.getenv("GITHUB_TOKEN")
    }
  }
}
