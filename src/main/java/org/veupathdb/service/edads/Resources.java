package org.veupathdb.service.edads;

import org.veupathdb.lib.container.jaxrs.config.Options;
import org.veupathdb.lib.container.jaxrs.server.ContainerResources;
import org.veupathdb.service.edads.service.AnalysesService;

/**
 * Service Resource Registration.
 *
 * This is where all the individual service specific resources and middleware
 * should be registered.
 */
public class Resources extends ContainerResources {

  private static final boolean DEVELOPMENT_MODE = true;

  public static final String SUBSETTING_SERVICE_URL;
  public static final String STREAM_PROCESSING_SERVICE_URL;

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

  public static boolean logResponseHeadersAsClient() {
    return DEVELOPMENT_MODE;
  }

  public Resources(Options opts) {
    super(opts);
    if (DEVELOPMENT_MODE) {
      enableJerseyTrace();
    }
  }

  /**
   * Returns an array of JaxRS endpoints, providers, and contexts.
   *
   * Entries in the array can be either classes or instances.
   */
  @Override
  protected Object[] resources() {
    return new Object[] {
      AnalysesService.class,
    };
  }
}
