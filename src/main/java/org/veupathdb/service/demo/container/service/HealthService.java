package org.veupathdb.service.demo.container.service;

import org.veupathdb.service.demo.container.health.Dependency.TestResult;
import org.veupathdb.service.demo.container.health.DependencyManager;
import org.veupathdb.service.demo.container.utils.Threads;
import org.veupathdb.service.demo.generated.model.DependencyStatus;
import org.veupathdb.service.demo.generated.model.DependencyStatus.OnlineType;
import org.veupathdb.service.demo.generated.model.DependencyStatusImpl;
import org.veupathdb.service.demo.generated.model.HealthResponse.StatusType;
import org.veupathdb.service.demo.generated.model.HealthResponseImpl;
import org.veupathdb.service.demo.generated.model.HealthResponseImpl.InfoTypeImpl;
import org.veupathdb.service.demo.generated.resources.Health;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.time.Duration;
import java.util.Map.Entry;
import java.util.stream.Collectors;

public class HealthService implements Health {
  private final DependencyManager depMan;

  public HealthService(DependencyManager depMan) {
    this.depMan = depMan;
  }

  @Override
  public GetHealthResponse getHealth() {
    var response = new HealthResponseImpl();
    var results = depMan.testDependencies();

    response.setStatus(StatusType.HEALTHY);
    response.setDependencies(results.entrySet()
      .stream()
      .map(HealthService::makeDependencyStatus)
      .peek(s -> {
        if (!s.getReachable() || s.getOnline() == OnlineType.UNKNOWN)
          response.setStatus(StatusType.UNHEALTHY);
      })
      .collect(Collectors.toList()));

    var uptime = ManagementFactory.getRuntimeMXBean().getUptime();
    var info = new InfoTypeImpl();
    info.setThreads(Threads.currentThreadCount());
    info.setUptime(durationString(uptime));
    info.setUptimeMillis(uptime);
    response.setInfo(info);

    return GetHealthResponse.respond200WithApplicationJson(response);
  }

  static DependencyStatus makeDependencyStatus(Entry<String, TestResult> e) {
    var out = new DependencyStatusImpl();
    out.setName(e.getKey());
    out.setReachable(e.getValue().isReachable());
    out.setOnline(e.getValue().isOnline());
    return out;
  }

  static String durationString(long duration) {
    var tmp = Duration.ofMillis(duration);
    var days = tmp.toDaysPart();
    var hours = tmp.toHoursPart();
    var minutes = tmp.toMinutesPart();
    var seconds = tmp.toSecondsPart();
    var millis = tmp.toMillisPart();
    if (days > 0)
      return String.format("%dd %dh %dm %d.%ds", days, hours, minutes, seconds, millis);
    if (hours > 0)
      return String.format("%dh %dm %d.%ds", hours, minutes, seconds, millis);
    if (minutes > 0)
      return String.format("%dm %d.%ds", minutes, seconds, millis);
    return String.format("%d.%ds", seconds, millis);
  }

}
