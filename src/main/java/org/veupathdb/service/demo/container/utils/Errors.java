package org.veupathdb.service.demo.container.utils;

import io.vulpine.lib.jcfi.CheckedRunnable;

public final class Errors {
  private Errors() {}

  public static void silence(CheckedRunnable fn) {
    try { fn.run(); }
    catch (Throwable ignored) {}
  }
}
