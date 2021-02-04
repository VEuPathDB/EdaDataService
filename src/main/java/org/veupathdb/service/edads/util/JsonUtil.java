package org.veupathdb.service.edads.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonUtil {

  public static String serialize(Object object) {
    try {
      return new ObjectMapper().writeValueAsString(object);
    }
    catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }
}
