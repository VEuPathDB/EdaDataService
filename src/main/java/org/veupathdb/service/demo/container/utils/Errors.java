package org.veupathdb.service.demo.container.utils;

import io.vulpine.lib.jcfi.CheckedRunnable;

public class Errors {
  public static void silence(CheckedRunnable fn) {
    try { fn.run(); }
    catch (Exception ignored) {}
  }
}
