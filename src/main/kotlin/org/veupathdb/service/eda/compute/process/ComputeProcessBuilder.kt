package org.veupathdb.service.eda.compute.process

import org.slf4j.LoggerFactory
import org.veupathdb.service.eda.compute.jobs.ReservedFiles
import java.nio.file.Path

/**
 * Process Builder for Compute Jobs
 *
 * This object is used to construct calls to external scripts and/or binaries
 * for a plugin job.
 *
 * `ComputeProcessBuilder` wraps a standard java [ProcessBuilder] and configures
 * it with basic properties such as the working directory and the stderr output
 * file.
 *
 * @author Elizabeth Paige Harper - https://github.com/foxcapades
 * @since 1.0.0
 */
@Suppress("unused")
class ComputeProcessBuilder(
  /**
   * Name of/path to the script or binary that will be called.
   */
  val command: String,

  /**
   * Local scratch workspace directory path.
   */
  val workDir: Path
) {

  private val Log = LoggerFactory.getLogger(javaClass)

  private val args = ArrayList<String>(10)
  private val env  = HashMap<String, String>()

  init {
    this.args.add(command)
    this.env["PATH"] = System.getenv("PATH")
  }

  /**
   * Adds a single argument to the call to be executed.
   *
   * @param arg Argument to add.
   *
   * @return This `ComputeProcessBuilder`.
   */
  fun addArg(arg: String): ComputeProcessBuilder {
    this.args.add(arg)
    return this
  }

  /**
   * Adds the given arguments to the call to be executed.
   *
   * @param args Arguments to add.
   *
   * @return This `ComputeProcessBuilder`.
   */
  fun addArgs(vararg args: String): ComputeProcessBuilder {
    this.args.addAll(args)
    return this
  }

  /**
   * Adds the given arguments to the call to be executed.
   *
   * @param args Arguments to add.
   *
   * @return This `ComputeProcessBuilder`.
   */
  fun addArgs(args: Iterable<String>): ComputeProcessBuilder {
    this.args.addAll(args)
    return this
  }

  /**
   * Sets the given environment variable on the call to be executed.
   *
   * @param variable Name of the environment variable to set.
   *
   * @param value Environment variable value to set.
   *
   * @return This `ComputeProcessBuilder`.
   */
  fun setEnv(variable: String, value: String): ComputeProcessBuilder {
    this.env[variable] = value
    return this
  }

  /**
   * Sets the given environment variables on the call to be executed.
   *
   * @param vars Map of environment variables to set.
   *
   * @return This `ComputeProcessBuilder`.
   */
  fun setEnv(vars: Map<String, String>): ComputeProcessBuilder {
    this.env.putAll(vars)
    return this
  }

  /**
   * Executes the configured command and waits for it to finish.
   *
   * @return The exit code of the command.
   */
  fun execute(): Int {
    Log.info("Executing command {}", command)
    Log.debug("Command arguments: {}", args)
    Log.debug("Command environment: {}", env)

    val proc = ProcessBuilder(args).also {
      it.environment().putAll(env)
      it.directory(workDir.toFile())
      it.redirectError(workDir.resolve(ReservedFiles.OutputErrors).toFile())
    }.start()

    return proc.waitFor()
  }
}