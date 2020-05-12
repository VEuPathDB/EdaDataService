package org.veupathdb.service.demo.container.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Aggregation of all external static function calls.  These are ugly and
 * untestable.
 */
public class Statics {
  private Statics() {}

  private static Statics instance;

  public Runtime runtime() {
    return Runtime.getRuntime();
  }

  public java.util.logging.LogManager javaLogManager() {
    return java.util.logging.LogManager.getLogManager();
  }

  public Logger getLogger(Class<?> c) {
    return LogManager.getLogger(c);
  }
  public Logger getLogger(String name) {
    return LogManager.getLogger(name);
  }
  public static Logger logger(Class<?> c) {
    return getInstance().getLogger(c);
  }
  public static Logger logger(String name) {
    return getInstance().getLogger(name);
  }

  public static Statics getInstance() {
    if (instance == null)
      instance = new Statics();

    return instance;
  }
}
