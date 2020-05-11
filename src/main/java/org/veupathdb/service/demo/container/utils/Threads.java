package org.veupathdb.service.demo.container.utils;

public class Threads {
  public static int currentThreadCount() {
    var group = Thread.currentThread().getThreadGroup();
    while (true) {
      var tmp = group.getParent();
      if (tmp == null)
        break;
      group = tmp;
    }
    return group.activeCount();
  }
}
