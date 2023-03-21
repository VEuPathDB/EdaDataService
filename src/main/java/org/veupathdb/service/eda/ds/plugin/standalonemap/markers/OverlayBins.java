package org.veupathdb.service.eda.ds.plugin.standalonemap.markers;

import org.veupathdb.service.eda.ds.plugin.standalonemap.StandaloneMapMarkersPlugin;
import org.veupathdb.service.eda.generated.model.CategoricalOverlayConfig;
import org.veupathdb.service.eda.generated.model.OverlayConfig;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import static org.veupathdb.service.eda.generated.model.OverlayType.CATEGORICAL;

public class OverlayBins {
  private final List<String> labels;
  private final List<NormalizedBinRange> binRanges;
  private final OverlayRecoder overlayRecoder;

  public OverlayBins(OverlayConfig overlayConfig) {
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
      case NUMBER -> input -> recode(Double.parseDouble(input));
      case INTEGER -> input -> recode(Integer.parseInt(input));
      case DATE -> input -> recode(Instant.parse(input).toEpochMilli());
      case CATEGORICAL -> input -> labels.contains(input) ? input : "__UNSELECTED__";
    };
  }

  public OverlayRecoder getOverlayRecoder() {
    return overlayRecoder;
  }

  private String recode(double varValue) {
    return binRanges.stream()
        .filter(bin -> bin.getStart() < varValue && bin.getEnd() > varValue)
        .findFirst()
        .orElseThrow()
        .getLabel();
  }
}
