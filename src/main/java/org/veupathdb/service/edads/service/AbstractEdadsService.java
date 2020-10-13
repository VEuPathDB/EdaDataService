package org.veupathdb.service.edads.service;

public abstract class AbstractEdadsService {

  protected static final String SUBSETTING_SERVICE_URL;
  protected static final String STREAM_PROCESSING_SERVICE_URL;

  static {
    SUBSETTING_SERVICE_URL = loadRequiredEnvVar("SUBSETTING_SERVICE_URL");
    STREAM_PROCESSING_SERVICE_URL = loadRequiredEnvVar("STREAM_PROCESSING_SERVICE_URL");
  }

  private static String loadRequiredEnvVar(String name) {
    String value = System.getenv(name);
    if (value == null) {
      throw new RuntimeException("Required environment variable '" + name + "' is not defined.");
    }
    return value;
  }

}
