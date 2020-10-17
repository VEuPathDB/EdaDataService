package org.veupathdb.service.edads.service;

import java.io.IOException;
import java.net.URL;

import org.veupathdb.service.edads.Resources;

import com.fasterxml.jackson.databind.ObjectMapper;

public class Utilities {

  public static <T> T getResponseObject(String urlPath, Class<T> responseObjectClass) {
    try {
      return new ObjectMapper().readerFor(responseObjectClass).readValue(new URL(Resources.SUBSETTING_SERVICE_URL + urlPath));
    }
    catch (IOException e) {
      throw new RuntimeException("Unable to read and reserialize studies endpoint response object", e);
    }
  }

}
