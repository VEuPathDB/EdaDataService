package org.veupathdb.service.eda.ds.plugin.standalonemap.markers;

import org.veupathdb.service.eda.generated.model.APIVariableType;
import org.veupathdb.service.eda.generated.model.ContinousOverlayConfig;

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

  public static List<NormalizedBinRange> fromOverlayConfig(ContinousOverlayConfig overlayConfig, String variableType) {
    if (variableType.equalsIgnoreCase(APIVariableType.DATE.getValue())) {
      return overlayConfig.getOverlayValues().stream()
          .map(binRange -> new NormalizedBinRange(
              Instant.parse(binRange.getMin()).toEpochMilli(),
              Instant.parse(binRange.getMax()).toEpochMilli(),
              binRange.getLabel()))
          .collect(Collectors.toList());
    } else {
      return overlayConfig.getOverlayValues().stream()
          .map(binRange -> new NormalizedBinRange(
              Double.parseDouble(binRange.getMin()),
              Double.parseDouble(binRange.getMax()),
              binRange.getLabel()))
          .collect(Collectors.toList());
    }
  }
}
