package org.veupathdb.service.demo.container.middleware;

import com.devskiller.friendly_id.FriendlyId;
import io.prometheus.client.Counter;
import io.prometheus.client.Histogram;
import io.prometheus.client.Histogram.Timer;

import javax.annotation.Priority;
import javax.ws.rs.container.*;
import javax.ws.rs.ext.Provider;

@Provider
@Priority(0)
@PreMatching
public class PrometheusFilter
implements ContainerRequestFilter, ContainerResponseFilter {
  private static final String TIME_KEY = FriendlyId.createFriendlyId();

  private static final Counter reqCount = Counter.build()
    .name("http_total_requests")
    .help("Total HTTP request count.")
    .labelNames("path", "method", "status")
    .register();
  private static final Histogram reqTime = Histogram.build()
    .name("http_request_duration")
    .help("Request times in milliseconds")
    .labelNames("path", "method")
    .register();


  @Override
  public void filter(ContainerRequestContext req) {
    req.setProperty(TIME_KEY, reqTime.labels(req.getUriInfo().getPath(),
      req.getMethod()).startTimer());
  }

  @Override
  public void filter(ContainerRequestContext req, ContainerResponseContext res) {
    ((Timer) req.getProperty(TIME_KEY)).observeDuration();
    reqCount.labels(req.getUriInfo().getPath(), req.getMethod(),
      String.valueOf(res.getStatus())).inc();
  }
}
