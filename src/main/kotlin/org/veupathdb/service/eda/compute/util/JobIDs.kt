package org.veupathdb.service.eda.compute.util

import org.apache.logging.log4j.LogManager
import org.gusdb.fgputil.json.JsonUtil
import org.json.JSONObject
import org.veupathdb.lib.hash_id.HashID
import org.veupathdb.lib.jackson.Json
import org.veupathdb.service.eda.generated.model.ComputeRequestBase
import java.util.*

object JobIDs {

  private val Log = LogManager.getLogger(javaClass)

  /**
   * Generates a job ID value from the given plugin name (url segment) and config
   * entity.
   *
   * Job IDs are generated from the MD5 hash of the following JSON array
   * structure:
   * ```json
   * [
   *   "plugin-url-segment",
   *   {
   *     "plugin": "configuration json",
   *     ...
   *   }
   * ]
   * ```
   *
   * @param pluginName URL segment of the plugin.
   *
   * @param entity HTTP compute request body.
   *
   * @return A [HashID] calculated from the given inputs.
   */
  fun of(pluginName: String, entity: ComputeRequestBase) : HashID {

    // fill null values with arrays so non-submission returns same hash as empty arrays
    if (entity.filters == null) entity.filters = Collections.emptyList()
    if (entity.derivedVariables == null) entity.derivedVariables = Collections.emptyList()

    // order of JSON object properties is not guaranteed in Jackson; use key-sorted serializer
    val unorderedString = Json.Mapper.writeValueAsString(entity)
    val orderedString = JsonUtil.serialize(JSONObject(unorderedString))

    // hash the plugin name with the full serialized config
    val jobId = HashID.ofMD5(Json.newArray(2) {
      add(pluginName)
      add(orderedString)
    })

    Log.info("Created job ID '$jobId' from pluginName '$pluginName' and serialized JSON: $orderedString")
    return jobId;
  }
}
