package org.veupathdb.service.demo.container.utils;

/**
 * Utilities for dealing with threads.
 */
public class Threads {
  private Threads() {}

  private static Threads instance;

  /**
   * Get the total current active thread count.
   *
   * Note: This method cannot be tested due to the Thread and ThreadGroup
   * methods being marked as final.
   */
  public int getCurrentThreadCount() {
    var group = Thread.currentThread().getThreadGroup();
    while (true) {
      var tmp = group.getParent();
      if (tmp == null)
        break;
      group = tmp;
    }
    return group.activeCount();
  }

  public static int currentThreadCount() {
    return getInstance().getCurrentThreadCount();
  }

  public static Threads getInstance() {
    if (instance == null)
      instance = new Threads();

    return instance;
  }
}
