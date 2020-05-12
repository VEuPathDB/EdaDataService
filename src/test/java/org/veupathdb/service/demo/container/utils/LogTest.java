package org.veupathdb.service.demo.container.utils;

import org.junit.jupiter.api.Test;

import java.util.logging.LogManager;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LogTest {

  @Test
  void initialize() throws Exception {
    var mk1 = mock(Statics.class);
    var mk2 = mock(LogManager.class);

    when(mk1.javaLogManager()).thenReturn(mk2);

    var in = Statics.class.getDeclaredField("instance");
    in.setAccessible(true);
    in.set(null, mk1);

    Log.initialize();

    verify(mk2, times(1)).reset();
  }
}
