package org.veupathdb.service.eda.compute.util

import com.fasterxml.jackson.annotation.JsonProperty
import org.gusdb.fgputil.Tuples.TwoTuple

data class AuthTuple(
  @JsonProperty("header")
  val header: String,
  @JsonProperty("token")
  val token: String
) {
  fun toFgpTuple() = TwoTuple(header, token)
}

internal fun TwoTuple<String, String>.toAuthTuple() = AuthTuple(first, second)
