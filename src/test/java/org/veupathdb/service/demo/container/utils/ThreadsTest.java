package org.veupathdb.service.demo.container.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ThreadsTest {

  @Test
  void currentThreadCount() throws Exception {
    var mk = mock(Threads.class);
    when(mk.getCurrentThreadCount()).thenReturn(22);

    var f = Threads.class.getDeclaredField("instance");
    f.setAccessible(true);
    f.set(null, mk);

    assertEquals(22, Threads.currentThreadCount());
  }

  @Test
  void getInstance() throws Exception {
    var f = Threads.class.getDeclaredField("instance");
    f.setAccessible(true);
    f.set(null, null);

    var a = Threads.getInstance();
    assertNotNull(a);
    assertSame(a, Threads.getInstance());
  }
}
