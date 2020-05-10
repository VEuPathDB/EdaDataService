package org.veupathdb.service.demo.container.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.yaml.YamlConfiguration;
//import org.slf4j.bridge.SLF4JBridgeHandler;
import org.veupathdb.service.demo.Main;

import java.io.IOException;

public class Log {
  /**
   * Routes all logging through Log4J2 and applies the configuration from
   * resources.
   */
  public static void initialize() throws IOException {
    java.util.logging.LogManager.getLogManager().reset();

//    SLF4JBridgeHandler.removeHandlersForRootLogger();
//    SLF4JBridgeHandler.install();
    Configurator.initialize(
      new YamlConfiguration((LoggerContext) LogManager.getContext(),
      new ConfigurationSource(Main.class.getResourceAsStream("/log4j2.yml"))));
  }
}
