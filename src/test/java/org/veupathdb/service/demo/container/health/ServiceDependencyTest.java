package org.veupathdb.service.demo.container.health;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.veupathdb.service.demo.container.health.Dependency.TestResult;
import org.veupathdb.service.demo.generated.model.DependencyStatus.OnlineType;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ServiceDependencyTest {

  Pinger pinger;

  @BeforeEach
  void setUp() {
    pinger = mock(Pinger.class);
  }

  @Test
  void getUrl() {
    var test = new ServiceDependency("", "foo", 0) {
      @Override TestResult serviceTest() { return  null; }
    };

    assertEquals("foo", test.getUrl());
  }

  @Test
  void getPort() {
    var test = new ServiceDependency("", "", 666) {
      @Override TestResult serviceTest() { return  null; }
    };

    assertEquals(666, test.getPort());
  }

  @Test
  void close() throws Exception {
    var test = new ServiceDependency("", "foo", 321) {
      @Override TestResult serviceTest() { return null; }
    };
    test.close(); // Empty method, nothing to test.
  }

  @Nested
  class TestFn {

    @Test
    void testNoPing() {
      when(pinger.isReachable("foo", 321)).thenReturn(false);

      var test = new ServiceDependency("", "foo", 321) {
        @Override TestResult serviceTest() { return null; }
      };
      test.setPinger(pinger);

      var res = test.test();

      assertFalse(res.isReachable());
      assertEquals(OnlineType.UNKNOWN, res.isOnline());
    }

    @Test
    void testPassthrough() {
      when(pinger.isReachable("foo", 321)).thenReturn(true);

      var value = new TestResult(true, OnlineType.UNKNOWN);

      var test = new ServiceDependency("", "foo", 321) {
        @Override TestResult serviceTest() { return value; }
      };
      test.setPinger(pinger);

      System.out.println(test.test().isReachable());
      System.out.println(test.test().isOnline());

      assertSame(value, test.test());
    }
  }
}
