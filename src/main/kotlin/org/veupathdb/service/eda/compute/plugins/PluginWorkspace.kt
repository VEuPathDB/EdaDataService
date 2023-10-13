package org.veupathdb.service.eda.compute.plugins

import com.fasterxml.jackson.databind.ObjectMapper
import org.rosuda.REngine.Rserve.RConnection
import org.rosuda.REngine.Rserve.RFileInputStream
import org.veupathdb.lib.compute.platform.job.JobWorkspace
import org.veupathdb.service.eda.compute.jobs.ReservedFiles
import org.veupathdb.service.eda.generated.model.ComputedVariableMetadata
import java.io.InputStream
import java.io.OutputStream
import java.io.Reader
import java.util.function.Consumer

/**
 * Plugin Workspace
 *
 * Wrapper for a plugin job's local scratch workspace.  Provides methods for
 * reading and writing files in the job workspace.
 *
 * @author Elizabeth Paige Harper - https://github.com/foxcapades
 * @since 1.0.0
 */
class PluginWorkspace(val internalWorkspace: JobWorkspace) : JobWorkspace by internalWorkspace {

  /**
   * Writes the given stream out to the file expected by the service for plugin
   * result data.
   *
   * @param data Stream of data to write out to the result data file.
   */
  fun writeDataResult(data: InputStream) {
    write(ReservedFiles.OutputTabular, data)
  }

  /**
   * Helper function that streams the resulting file of the R command
   * into the job's data result file.
   *
   * @param connection an R connection
   * @param cmd R command whose result is a file reference in the R workspace
   */
  fun writeDataResult(connection: RConnection, cmd: String) {
    val outFile = connection.eval(cmd).asString();
    writeDataResult(connection.openFile(outFile));
  }

  /**
   * Uses the passed consumer to write plugin result data out to the data file.
   *
   * @param consumer Function that will write data out to the result data file.
   */
  fun writeDataResult(consumer: Consumer<OutputStream>) {
    write(ReservedFiles.OutputTabular, consumer)
  }

  /**
   * Writes the given stream out to the file expected by the service for plugin
   * result data.
   *
   * @param data Stream of data to write out to the result data file.
   */
  fun writeDataResult(data: Reader) {
    write(ReservedFiles.OutputTabular, data)
  }

  /**
   * Writes the given string out to the file expected by the service for plugin
   * result data.
   *
   * @param data Data to write out to the result data file.
   */
  fun writeDataResult(data: String) {
    write(ReservedFiles.OutputTabular, data)
  }

  /**
   * Writes the given stream out to the file expected by the service for plugin
   * result metadata.
   *
   * @param data Stream of data to write out to the result metadata file.
   */
  fun writeMetaResult(data: InputStream) {
    write(ReservedFiles.OutputMeta, data)
  }

  /**
   * Helper function that streams the resulting file of the R command
   * into the job's meta result file.
   *
   * @param connection an R connection
   * @param cmd R command whose result is a file reference in the R workspace
   */
  fun writeMetaResult(connection: RConnection, cmd: String) {
    val outFile = connection.eval(cmd).asString();
    writeMetaResult(connection.openFile(outFile));
  }

  /**
   * Writes the given stream out to the file expected by the service for plugin
   * result metadata.
   *
   * @param data Stream of data to write out to the result metadata file.
   *//*
  fun writeMetaResult(data: Reader)  {
    write(ReservedFiles.OutputMeta, data)
  }*/

  /**
   * Writes the given string out to the file expected by the service for plugin
   * result metadata.
   *
   * @param data Data to write out to the result metadata file.
   *//*
  fun writeMetaResult(data: String)  {
    write(ReservedFiles.OutputMeta, data)
  }*/

  /**
   * Writes the given metadata object out to the file expected by the service for plugin
   * result metadata.
   *
   * @param data Metadata to write out to the result metadata file.
   */
  fun writeMetaResult(data: ComputedVariableMetadata)  {
    write(ReservedFiles.OutputMeta, ObjectMapper().writeValueAsString(data))
  }

  /**
   * Writes the given stream out to the file expected by the service for plugin
   * result statistics.
   *
   * @param data Stream of data to write out to the result statistics file.
   */
  fun writeStatisticsResult(data: InputStream) {
    write(ReservedFiles.OutputStats, data)
  }

  /**
   * Helper function that streams the resulting file of the R command
   * into the job's statistics result file.
   *
   * @param connection an R connection
   * @param cmd R command whose result is a file reference in the R workspace
   */
  fun writeStatisticsResult(connection: RConnection, cmd: String) {
    val outFile = connection.eval(cmd).asString();
    writeStatisticsResult(connection.openFile(outFile));
  }

  /**
   * Writes the given stream out to the file expected by the service for plugin
   * result statistics.
   *
   * @param data Stream of data to write out to the result statistics file.
   */
  fun writeStatisticsResult(data: Reader) {
    write(ReservedFiles.OutputStats, data)
  }

  /**
   * Writes the given string out to the file expected by the service for plugin
   * result statistics.
   *
   * @param data Data to write out to the result statistics file.
   */
  fun writeStatisticsResult(data: String) {
    write(ReservedFiles.OutputStats, data)
  }

  /**
   * Uses the passed consumer to write plugin result statistics data out to a file.
   *
   * @param consumer Function that will write stats out to the result stats file.
   */
  fun writeStatisticsResult(consumer: Consumer<OutputStream>) {
    write(ReservedFiles.OutputStats, consumer)
  }
}
