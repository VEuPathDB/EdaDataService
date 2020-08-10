import java.util.Properties
import java.io.FileInputStream

val buildProps = Properties()
buildProps.load(FileInputStream(File(rootDir, "service.properties")))

rootProject.name = buildProps.getProperty("project.name")
  ?: error("failed to retrieve project name")

val core = file("../lib-jaxrs-container-core");
if (core.exists()) {
  include(":core")
  project(":core").projectDir = core
}
