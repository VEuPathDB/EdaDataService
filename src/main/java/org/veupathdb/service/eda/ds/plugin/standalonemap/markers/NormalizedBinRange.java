package org.veupathdb.service.eda.ds.plugin.standalonemap.markers;

import org.veupathdb.service.eda.generated.model.DateOverlayConfig;
import org.veupathdb.service.eda.generated.model.NumericOverlayConfig;
import org.veupathdb.service.eda.generated.model.OverlayConfig;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class NormalizedBinRange {
  private final double start;
  private final double end;
  private final String label;

  public NormalizedBinRange(double start, double end, String label) {
    this.start = start;
    this.end = end;
    this.label = label;
  }

  public double getStart() {
    return start;
  }

  public double getEnd() {
    return end;
  }

  public String getLabel() {
    return label;
  }

  public static List<NormalizedBinRange> fromOverlayConfig(OverlayConfig overlayConfig) {
    switch (overlayConfig.getOverlayType()) {
      case DATE -> {
        DateOverlayConfig dateOverlayConfig = (DateOverlayConfig) overlayConfig;
        return dateOverlayConfig.getOverlayValues().stream()
            .map(binRange -> new NormalizedBinRange(
                Instant.parse(binRange.getBinStart()).toEpochMilli(),
                Instant.parse(binRange.getBinEnd()).toEpochMilli(),
                binRange.getBinLabel()))
            .collect(Collectors.toList());
      }
      case NUMBER -> {
        NumericOverlayConfig numericOverlayConfig = (NumericOverlayConfig) overlayConfig;
        return numericOverlayConfig.getOverlayValues().stream()
            .map(binRange -> new NormalizedBinRange(
                binRange.getBinStart().doubleValue(),
                binRange.getBinEnd().doubleValue(),
                binRange.getBinLabel()))
            .collect(Collectors.toList());
      }
    }
    return Collections.emptyList();
  }
}
