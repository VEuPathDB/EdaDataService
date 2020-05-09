package org.veupathdb.service.demo.middleware;

import org.veupathdb.service.demo.utils.RequestKeys;

import javax.annotation.Priority;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.ext.Provider;
import java.util.UUID;

/**
 * Assigns a unique ID to each request for logging, error tracing purposes.
 */
@Provider
@Priority(Integer.MIN_VALUE)
public class RequestIdFilter implements ContainerRequestFilter {
  @Override
  public void filter(ContainerRequestContext req) {
    req.setProperty(RequestKeys.REQUEST_ID, UUID.randomUUID().toString());
  }
}
