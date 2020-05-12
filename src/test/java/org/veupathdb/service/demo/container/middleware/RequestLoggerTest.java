package org.veupathdb.service.demo.container.middleware;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.util.Supplier;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.veupathdb.service.demo.container.utils.Statics;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class RequestLoggerTest {

  @Test
  void filter() throws Exception {
    var stat = mock(Statics.class);
    var log  = mock(Logger.class);
    var req  = mock(ContainerRequestContext.class);
    var uri  = mock(UriInfo.class);

    when(stat.getLogger(RequestLogger.class)).thenReturn(log);
    when(req.getMethod()).thenReturn("");
    when(req.getUriInfo()).thenReturn(uri);
    when(uri.getPath()).thenReturn("");

    var i = Statics.class.getDeclaredField("instance");
    i.setAccessible(true);
    i.set(null, stat);

    new RequestLogger().filter(req);

    verify(stat, times(1)).getLogger(RequestLogger.class);
    verify(log, times(1)).debug((String) any(), any(), any());
  }

  @Test
  void testFilterDebug() throws Exception {
    var stat = mock(Statics.class);
    var log  = mock(Logger.class);
    var req  = mock(ContainerRequestContext.class);
    var res  = mock(ContainerResponseContext.class);
    var uri  = mock(UriInfo.class);

    when(stat.getLogger(RequestLogger.class)).thenReturn(log);
    when(req.getMethod()).thenReturn("");
    when(req.getUriInfo()).thenReturn(uri);
    when(uri.getPath()).thenReturn("");
    when(res.getStatusInfo()).thenReturn(Status.OK);
    when(res.getStatus()).thenReturn(0);

    var i = Statics.class.getDeclaredField("instance");
    i.setAccessible(true);
    i.set(null, stat);

    new RequestLogger().filter(req, res);

    verify(stat, times(1)).getLogger(RequestLogger.class);
    verify(log, times(1)).debug((Supplier<?>) any());
  }


  @Test
  @SuppressWarnings("unchecked")
  void testFilterWarn() throws Exception {
    var stat = mock(Statics.class);
    var log  = mock(Logger.class);
    var req  = mock(ContainerRequestContext.class);
    var res  = mock(ContainerResponseContext.class);
    var uri  = mock(UriInfo.class);

    when(stat.getLogger(RequestLogger.class)).thenReturn(log);
    when(req.getMethod()).thenReturn("");
    when(req.getUriInfo()).thenReturn(uri);
    when(uri.getPath()).thenReturn("");
    when(res.getStatusInfo()).thenReturn(Status.INTERNAL_SERVER_ERROR);
    when(res.getStatus()).thenReturn(0);

    var i = Statics.class.getDeclaredField("instance");
    i.setAccessible(true);
    i.set(null, stat);

    new RequestLogger().filter(req, res);

    ArgumentCaptor<Supplier<?>> capt = ArgumentCaptor.forClass(Supplier.class);

    verify(stat, times(1)).getLogger(RequestLogger.class);
    verify(log, times(1)).warn(capt.capture());
    assertTrue(capt.getValue().get() instanceof String);
  }
}
