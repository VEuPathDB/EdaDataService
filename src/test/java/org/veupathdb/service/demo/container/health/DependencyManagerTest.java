package org.veupathdb.service.demo.container.health;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.veupathdb.service.demo.container.health.Dependency.TestResult;
import org.veupathdb.service.demo.generated.model.DependencyStatus.OnlineType;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DependencyManagerTest {

  DependencyManager test;

  @BeforeEach
  void setUp() throws Exception {
    var m = DependencyManager.class.getDeclaredConstructor();
    m.setAccessible(true);

    test = m.newInstance();
  }

  @Test
  @SuppressWarnings("unchecked")
  void register() throws Exception {
    var dep = new DatabaseDependency("foo", "", 123, null);
    test.register(dep);

    var f = test.getClass().getDeclaredField("dependencies");
    f.setAccessible(true);

    var foo = (Map<String, Dependency>) f.get(test);

    assertSame(foo.get("foo"), dep);

    assertThrows(RuntimeException.class, () -> test.register(dep));
  }

  @Test
  void testDependencies() {
    var dep = mock(DatabaseDependency.class);
    var val = new TestResult(false, OnlineType.YES);
    when(dep.getName()).thenReturn("foo");
    when(dep.test()).thenReturn(val);

    test.register(dep);
    var res = test.testDependencies();
    assertSame(res.get("foo"), val);
  }

  @Test
  void shutDown() throws Exception {
    var dep1 = mock(DatabaseDependency.class);
    var dep2 = mock(DatabaseDependency.class);

    when(dep1.getName()).thenReturn("foo");
    when(dep2.getName()).thenReturn("bar");

    doThrow(new Exception()).when(dep2).close();

    test.register(dep1);
    test.register(dep2);
    test.shutDown();

    verify(dep1).close();
    verify(dep2).close();
  }

  @Test
  void getInstance() {
    assertSame(DependencyManager.getInstance(), DependencyManager.getInstance());
  }
}
