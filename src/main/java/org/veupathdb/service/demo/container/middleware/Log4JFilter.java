package org.veupathdb.service.demo.container.middleware;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.veupathdb.service.demo.container.utils.RequestKeys;

import javax.annotation.Priority;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.Response.Status.Family;
import javax.ws.rs.ext.Provider;
import java.util.function.Consumer;

import static java.lang.String.format;

@Provider
@Priority(0)
public class Log4JFilter implements ContainerRequestFilter, ContainerResponseFilter {
  private static final String
    START_FORMAT = "%s Request start: %s %s",
    END_FORMAT   = "%s Request end: %s %s %d";

  private static final Logger LOG = LogManager.getLogger(Log4JFilter.class);

  @Override
  public void filter(ContainerRequestContext req) {
    if (LOG.isDebugEnabled())
      LOG.debug(format(START_FORMAT, req.getProperty(RequestKeys.REQUEST_ID),
        req.getMethod(), req.getUriInfo().getPath()));
  }

  @Override
  public void filter(ContainerRequestContext req, ContainerResponseContext res) {
    final Consumer<String> fn;
    if (res.getStatusInfo().getFamily() == Family.SERVER_ERROR) {
      fn = LOG::warn;
    } else {
      fn = LOG::debug;
    }

    fn.accept(format(END_FORMAT, req.getProperty(RequestKeys.REQUEST_ID),
      req.getMethod(), req.getUriInfo().getPath(), res.getStatus()));
  }
}
