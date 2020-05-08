import java.util.Properties
import java.io.FileInputStream
import java.net.URI

val buildProps = Properties()
buildProps.load(FileInputStream(File(rootDir, "service.properties")))

rootProject.name = buildProps.getProperty("project.name")
  ?: error("failed to retrieve project name")

sourceControl {
  gitRepository(URI.create("https://github.com/VEuPathDB/FgpUtil.git")) {
    producesModule("org.gusdb:fgputil")
  }
}
