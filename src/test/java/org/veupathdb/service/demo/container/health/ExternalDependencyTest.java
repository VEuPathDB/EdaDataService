package org.veupathdb.service.demo.container.health;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ExternalDependencyTest {

  @Test
  void setPinger() throws Exception {
    var test = new ExternalDependency("") {
      @Override public void close() {}
      @Override public TestResult test() { return null; }
    };
    var input = new Pinger();

    test.setPinger(input);

    var field = ExternalDependency.class.getDeclaredField("pinger");
    field.setAccessible(true);
    assertEquals(input, field.get(test));
  }
}
