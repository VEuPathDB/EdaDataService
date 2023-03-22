package org.veupathdb.service.eda.ds.plugin.standalonemap.markers;

import org.veupathdb.service.eda.generated.model.CategoricalOverlayConfig;
import org.veupathdb.service.eda.generated.model.OverlayConfig;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import static org.veupathdb.service.eda.generated.model.OverlayType.CATEGORICAL;

public class OverlaySpecification {
  private final List<String> labels;
  private final List<NormalizedBinRange> binRanges;
  private final OverlayRecoder overlayRecoder;

  public OverlaySpecification(OverlayConfig overlayConfig) {
    if (CATEGORICAL.equals(overlayConfig.getOverlayType())) {
      binRanges = null;
      labels = ((CategoricalOverlayConfig) overlayConfig).getOverlayValues();
    } else {
      binRanges = NormalizedBinRange.fromOverlayConfig(overlayConfig);
      labels = binRanges.stream()
          .map(NormalizedBinRange::getLabel)
          .collect(Collectors.toList());
    }
    overlayRecoder = switch (overlayConfig.getOverlayType()) {
      case NUMBER -> input -> recodeNumeric(Double.parseDouble(input));
      // This is unused, we probably want to distinguish numbers from integers to avoid incurring the cost of parsing a double for int vars.
      case INTEGER -> input -> recodeNumeric(Integer.parseInt(input));
      case DATE -> input -> recodeNumeric(Instant.parse(input).toEpochMilli());
      case CATEGORICAL -> input -> labels.contains(input) ? input : "__UNSELECTED__";
    };
  }

  public String recode(String s) {
    if (s == null || s.isEmpty()) {
      return null;
    }
    return overlayRecoder.recode(s);
  }

  private String recodeNumeric(double varValue) {
    // Binary search
    return binRanges.stream()
        .filter(bin -> bin.getStart() < varValue && bin.getEnd() > varValue)
        .findFirst()
        .orElseThrow()
        .getLabel();
  }
}
