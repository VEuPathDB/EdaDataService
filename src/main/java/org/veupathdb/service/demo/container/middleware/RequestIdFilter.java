package org.veupathdb.service.demo.container.middleware;

import com.devskiller.friendly_id.FriendlyId;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.veupathdb.service.demo.container.Globals;
import org.veupathdb.service.demo.container.utils.RequestKeys;

import javax.annotation.Priority;
import javax.ws.rs.container.*;
import javax.ws.rs.ext.Provider;

import java.io.IOException;
import java.util.UUID;

/**
 * Assigns a unique ID to each request for logging, error tracing purposes.
 */
@Provider
@Priority(1)
@PreMatching
public class RequestIdFilter
implements ContainerRequestFilter, ContainerResponseFilter {
  private final Logger log = LogManager.getLogger(RequestIdFilter.class);

  @Override
  public void filter(ContainerRequestContext req) {
    log.trace("RequestIdFilter#filter(req)");
    var id = FriendlyId.createFriendlyId();
    req.setProperty(RequestKeys.REQUEST_ID, id);
    ThreadContext.put(Globals.CONTEXT_ID, id);
  }

  @Override
  public void filter(ContainerRequestContext req, ContainerResponseContext res) {
    log.trace("RequestIdFilter#filter(req, res)");
    ThreadContext.remove(Globals.CONTEXT_ID);
  }
}
