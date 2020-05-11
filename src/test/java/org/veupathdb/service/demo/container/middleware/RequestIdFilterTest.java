package org.veupathdb.service.demo.container.middleware;

import org.apache.logging.log4j.ThreadContext;
import org.junit.jupiter.api.Test;
import org.veupathdb.service.demo.container.Globals;
import org.veupathdb.service.demo.container.utils.RequestKeys;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RequestIdFilterTest {

  @Test
  void filter() {
    var req = mock(ContainerRequestContext.class);
    var test = new RequestIdFilter();

    test.filter(req);

    verify(req, times(1)).setProperty(eq(RequestKeys.REQUEST_ID), any(String.class));
    assertNotNull(ThreadContext.get(Globals.CONTEXT_ID));

    ThreadContext.remove(Globals.CONTEXT_ID);
  }

  @Test
  void testFilter() {
    var req = mock(ContainerRequestContext.class);
    var res = mock(ContainerResponseContext.class);
    var test = new RequestIdFilter();

    ThreadContext.put(Globals.CONTEXT_ID, "foo");

    test.filter(req, res);

    assertNull(ThreadContext.get(Globals.CONTEXT_ID));
  }
}
