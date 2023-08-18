package org.veupathdb.service.eda.ds.plugin.standalonemap.markers;

import org.veupathdb.service.eda.ds.utils.CommonFormats;
import org.veupathdb.service.eda.generated.model.APIVariableType;
import org.veupathdb.service.eda.generated.model.ContinousOverlayConfig;

import java.time.LocalDate;
import java.time.ZoneOffset;
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
              LocalDate.parse(binRange.getBinStart(), CommonFormats.TABULAR_DATE_FORMAT).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(),
              LocalDate.parse(binRange.getBinEnd(), CommonFormats.TABULAR_DATE_FORMAT).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(),
              binRange.getBinLabel()))
          .collect(Collectors.toList());
    } else {
      return overlayConfig.getOverlayValues().stream()
          .map(binRange -> new NormalizedBinRange(
              Double.parseDouble(binRange.getBinStart()),
              Double.parseDouble(binRange.getBinEnd()),
              binRange.getBinLabel()))
          .collect(Collectors.toList());
    }
  }
}
