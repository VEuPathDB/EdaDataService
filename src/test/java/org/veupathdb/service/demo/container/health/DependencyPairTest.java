package org.veupathdb.service.demo.container.health;

import org.junit.jupiter.api.Test;
import org.veupathdb.service.demo.container.health.Dependency.TestResult;
import org.veupathdb.service.demo.generated.model.DependencyStatus.OnlineType;

import static org.junit.jupiter.api.Assertions.*;

class DependencyPairTest {

  @Test
  void constructor() {
    var res  = new TestResult(false, OnlineType.YES);
    var test = new DependencyPair("name", res);

    assertEquals("name", test.name);
    assertSame(res, test.result);
  }
}
