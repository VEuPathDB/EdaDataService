package org.veupathdb.service.eda.data.plugin.standalonemap.markers;

import org.veupathdb.service.eda.generated.model.APIVariableType;
import org.veupathdb.service.eda.generated.model.ContinousOverlayConfig;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class NormalizedBinRange {

  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

  private final double _min;
  private final double _max;
  private final String _label;

  public NormalizedBinRange(double min, double max, String label) {
    _min = min;
    _max = max;
    _label = label;
  }

  public double getMin() {
    return _min;
  }

  public double getMax() {
    return _max;
  }

  public String getLabel() {
    return _label;
  }

  public static List<NormalizedBinRange> fromOverlayConfig(ContinousOverlayConfig overlayConfig, String variableType) {
    if (variableType.equalsIgnoreCase(APIVariableType.DATE.getValue())) {
      return overlayConfig.getOverlayValues().stream()
          .map(binRange -> new NormalizedBinRange(
              LocalDate.parse(binRange.getBinStart(), DATE_FORMATTER).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(),
              LocalDate.parse(binRange.getBinEnd(), DATE_FORMATTER).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(),
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
