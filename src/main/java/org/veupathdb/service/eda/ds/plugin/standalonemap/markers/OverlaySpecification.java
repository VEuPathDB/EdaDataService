package org.veupathdb.service.eda.ds.plugin.standalonemap.markers;

import org.veupathdb.service.eda.generated.model.APIVariableType;
import org.veupathdb.service.eda.generated.model.CategoricalOverlayConfig;
import org.veupathdb.service.eda.generated.model.ContinousOverlayConfig;
import org.veupathdb.service.eda.generated.model.OverlayConfig;
import org.veupathdb.service.eda.generated.model.VariableSpec;

import java.time.Instant;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.veupathdb.service.eda.generated.model.OverlayType.CATEGORICAL;

public class OverlaySpecification {
  private final List<String> labels;
  private final List<NormalizedBinRange> binRanges;
  private OverlayRecoder overlayRecoder;

  public OverlaySpecification(OverlayConfig overlayConfig, Function<VariableSpec, String> varTypeFinder) {
    final String variableType = varTypeFinder.apply(overlayConfig.getOverlayVariable());
    if (CATEGORICAL.equals(overlayConfig.getOverlayType())) {
      binRanges = null;
      labels = ((CategoricalOverlayConfig) overlayConfig).getOverlayValues();
    } else {
      binRanges = NormalizedBinRange.fromOverlayConfig((ContinousOverlayConfig) overlayConfig, variableType);
      labels = binRanges.stream()
          .map(NormalizedBinRange::getLabel)
          .collect(Collectors.toList());
    }
    if (variableType.equalsIgnoreCase(APIVariableType.NUMBER.getValue())
        || variableType.equalsIgnoreCase(APIVariableType.LONGITUDE.getValue())) {
      overlayRecoder = input -> recodeNumeric(Double.parseDouble(input));
    } else if (variableType.equalsIgnoreCase(APIVariableType.INTEGER.getValue())) {
      overlayRecoder = input -> recodeNumeric(Integer.parseInt(input));
    } else if (variableType.equalsIgnoreCase(APIVariableType.DATE.getValue())) {
      overlayRecoder = input -> recodeNumeric(Instant.parse(input).toEpochMilli());
    } else {
      overlayRecoder = input -> labels.contains(input) ? input : "__UNSELECTED__";
    }
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
