package org.veupathdb.service.demo.container.middleware;

import com.devskiller.friendly_id.FriendlyId;
import io.prometheus.client.Counter;
import io.prometheus.client.Histogram;
import io.prometheus.client.Histogram.Timer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Priority;
import javax.ws.rs.container.*;
import javax.ws.rs.ext.Provider;

@Provider
@Priority(1)
@PreMatching
public class PrometheusFilter
implements ContainerRequestFilter, ContainerResponseFilter {
  private static final Logger LOG = LogManager.getLogger(PrometheusFilter.class);
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
    .buckets(0.005, 0.01, 0.1, 0.5, 1, 5, 10, Double.POSITIVE_INFINITY)
    .register();


  @Override
  public void filter(ContainerRequestContext req) {
    LOG.trace("PrometheusFilter#filter(req)");
    req.setProperty(TIME_KEY, reqTime.labels(req.getUriInfo().getPath(),
      req.getMethod()).startTimer());
  }

  @Override
  public void filter(ContainerRequestContext req, ContainerResponseContext res) {
    LOG.trace("PrometheusFilter#filter(req, res)");
    ((Timer) req.getProperty(TIME_KEY)).observeDuration();
    reqCount.labels(req.getUriInfo().getPath(), req.getMethod(),
      String.valueOf(res.getStatus())).inc();
  }
}
