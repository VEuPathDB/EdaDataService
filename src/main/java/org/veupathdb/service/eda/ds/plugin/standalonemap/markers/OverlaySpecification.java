package org.veupathdb.service.eda.ds.plugin.standalonemap.markers;

import org.veupathdb.service.eda.generated.model.APIVariableDataShape;
import org.veupathdb.service.eda.generated.model.APIVariableType;
import org.veupathdb.service.eda.generated.model.CategoricalOverlayConfig;
import org.veupathdb.service.eda.generated.model.ContinousOverlayConfig;
import org.veupathdb.service.eda.generated.model.OverlayConfig;
import org.veupathdb.service.eda.generated.model.VariableSpec;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.veupathdb.service.eda.generated.model.OverlayType.CATEGORICAL;

public class OverlaySpecification {
  private final List<String> labels;
  private final List<NormalizedBinRange> binRanges;
  private final VariableSpec overlayVariable;
  private OverlayRecoder overlayRecoder;

  public OverlaySpecification(OverlayConfig overlayConfig,
                              Function<VariableSpec, String> varTypeFinder,
                              Function<VariableSpec, String> varDatashapeFinder) {
    final String variableType = varTypeFinder.apply(overlayConfig.getOverlayVariable());
    this.overlayVariable = overlayConfig.getOverlayVariable();
    if (CATEGORICAL.equals(overlayConfig.getOverlayType())) {
      validateCategoricalOverlayValues((CategoricalOverlayConfig) overlayConfig, varDatashapeFinder);
      binRanges = null;
      labels = ((CategoricalOverlayConfig) overlayConfig).getOverlayValues();
    } else {
      binRanges = NormalizedBinRange.fromOverlayConfig((ContinousOverlayConfig) overlayConfig, variableType);
      validateContinuousBinRanges((ContinousOverlayConfig) overlayConfig, binRanges, varDatashapeFinder);
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
      overlayRecoder = input -> recodeNumeric(LocalDateTime.parse(input).toInstant(ZoneOffset.UTC).toEpochMilli());
    } else {
      overlayRecoder = input -> labels.contains(input) ? input : "__UNSELECTED__";
    }
  }

  public String getRBinListAsString() {
    String rBinList = "veupathUtils::BinList(S4Vectors::SimpleList(";

    boolean first = true;
    for (int i = 0; i < labels.size(); i++) {
      String rBin = "veupathUtils::Bin(binLabel='" + labels.get(i) + "'";
      if (binRanges != null) {
        rBin += ",binStart=" + String.valueOf(binRanges.get(i).getStart()) + 
                ",binEnd=" + String.valueOf(binRanges.get(i).getEnd());
      }
      rBin += ")";

      if (first) {
        rBinList += rBin;
        first = false;
      } else {
        rBinList += "," + rBin;
      }
    }

    return rBinList + "))";
  }

  public VariableSpec getOverlayVariable() {
    return overlayVariable;
  }

  public String recode(String s) {
    if (s == null || s.isEmpty()) {
      return null;
    }
    return overlayRecoder.recode(s);
  }

  private void validateCategoricalOverlayValues(CategoricalOverlayConfig overlayConfig, Function<VariableSpec, String> varSpecFinder) {
    String overlayShape = varSpecFinder.apply(overlayConfig.getOverlayVariable());
    if (!overlayShape.equalsIgnoreCase(APIVariableDataShape.CATEGORICAL.toString()) &&
        !overlayShape.equalsIgnoreCase(APIVariableDataShape.BINARY.toString()) &&
        !overlayShape.equalsIgnoreCase(APIVariableDataShape.ORDINAL.toString())) {
      throw new IllegalArgumentException("Input overlay variable %s is %s, but provided overlay configuration is for a categorical variable"
          .formatted(overlayConfig.getOverlayVariable().getVariableId(), varSpecFinder.apply(overlayConfig.getOverlayVariable())));
    }
    int numDistinctOverlayVals = new HashSet<>(overlayConfig.getOverlayValues()).size();
    if (numDistinctOverlayVals != overlayConfig.getOverlayValues().size()) {
      throw new IllegalArgumentException("All overlay values must be unique: " + overlayConfig.getOverlayValues());
    }
  }

  private void validateContinuousBinRanges(ContinousOverlayConfig overlayConfig, List<NormalizedBinRange> normalizedRanges, Function<VariableSpec, String> varSpecFinder) {
    final String dataShape = varSpecFinder.apply(overlayConfig.getOverlayVariable());
    if (!dataShape.equalsIgnoreCase(APIVariableDataShape.CONTINUOUS.toString())) {
      throw new IllegalArgumentException("Input overlay variable %s is %s, but provided overlay configuration is for a continuous variable"
          .formatted(overlayConfig.getOverlayVariable().getVariableId(), dataShape));
    }
    boolean anyMissingBinStart = overlayConfig.getOverlayValues().stream().anyMatch(bin -> bin.getBinStart() == null);
    boolean anyMissingBinEnd = overlayConfig.getOverlayValues().stream().anyMatch(bin -> bin.getBinEnd() == null);
    if (anyMissingBinStart || anyMissingBinEnd) {
      throw new IllegalArgumentException("All numeric bin ranges must have start and end.");
    }
    Map<Double, Double> binEdges = normalizedRanges.stream()
        .collect(Collectors.toMap(
            NormalizedBinRange::getStart,
            NormalizedBinRange::getEnd,
            (u, v) -> {
               throw new IllegalStateException(String.format("Duplicate key %s", u));
            }, 
            LinkedHashMap::new));
    boolean first = true;
    Double prevBinEnd = null;
    for (Double binStart : binEdges.keySet()) {
      if (first) {
        first = false;
      } else if (prevBinEnd > binStart) {
        throw new IllegalArgumentException("Bin Ranges must not overlap");
      }
      prevBinEnd = binEdges.get(binStart);
    }
  }

  private String recodeNumeric(double varValue) {
    // Binary search?
    return binRanges.stream()
        .filter(bin -> bin.getLabel().startsWith("[") ? 
                       bin.getStart() <= varValue && bin.getEnd() >= varValue : 
                       bin.getStart() < varValue && bin.getEnd() >= varValue)
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("The variable value " + varValue + " is not in any specified bin range."))
        .getLabel();
  }
}
