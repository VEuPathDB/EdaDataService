package org.veupathdb.service.eda.compute.jobs

/**
 * Index of file names that are reserved for internal use by the EDA Compute
 * service.
 *
 * @author Elizabeth Paige Harper - https://github.com/foxcapades
 * @since 1.0.0
 */
object ReservedFiles {
  const val InputMeta = "input-meta"

  // Not used by this service directly, reserved by the AsyncPlatform.
  const val InputConfig = "input-config"
  const val InputRequest = "input-request"

  const val OutputStats = "output-stats"
  const val OutputMeta = "output-meta"
  const val OutputTabular = "output-data"
  const val OutputErrors = "error.log"
  const val OutputException = "exception.log"

  /**
   * Tests whether the given [name] string collides with one of the defined
   * "reserved" file names.
   *
   * @param name Name to test.
   *
   * @return `true` if the given [name] is a reserved file name, otherwise
   * `false`.
   */
  @JvmStatic
  fun isReservedFileName(name: String): Boolean {
    return when (name) {
      InputMeta       -> true
      InputConfig     -> true
      InputRequest    -> true
      OutputStats     -> true
      OutputMeta      -> true
      OutputTabular   -> true
      OutputErrors    -> true
      OutputException -> true
      else            -> false
    }
  }
}