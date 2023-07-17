package org.veupathdb.service.eda.ds.plugin.standalonemap.markers;

import org.veupathdb.service.eda.generated.model.APIVariableDataShape;
import org.veupathdb.service.eda.generated.model.CategoricalQuantitativeOverlayConfig;
import org.veupathdb.service.eda.generated.model.ContinuousQuantitativeOverlayConfig;
import org.veupathdb.service.eda.generated.model.QuantitativeOverlayConfig;
import org.veupathdb.service.eda.generated.model.VariableSpec;

import java.util.HashSet;
import java.util.function.Function;

import static org.veupathdb.service.eda.generated.model.OverlayType.CATEGORICAL;

public class QuantitativeAggregateConfiguration {
  private final VariableSpec overlayVariable;
  private final Function<Integer, MarkerAggregator<Double>> aggregatorSupplier;
  private Function<String, Double> variableValueQuantifier;

  /**
   * Constructs a map bubble specification from the raw input. Note that this will throw an IllegalArgumentException
   * with a user-friendly message if there are any user input errors.
   */
  public QuantitativeAggregateConfiguration(QuantitativeOverlayConfig overlayConfig,
                                            Function<VariableSpec, String> varShapeFinder) {
    this.overlayVariable = overlayConfig.getOverlayVariable();
    if (CATEGORICAL.equals(overlayConfig.getOverlayType())) {
      if (!varShapeFinder.apply(overlayVariable).equalsIgnoreCase(APIVariableDataShape.CATEGORICAL.getValue())
          && !varShapeFinder.apply(overlayVariable).equalsIgnoreCase(APIVariableDataShape.ORDINAL.getValue())) {
        throw new IllegalArgumentException("Incorrect overlay configuration type for categorical var: " + varShapeFinder.apply(overlayVariable));
      }
      CategoricalQuantitativeOverlayConfig colorConfig = (CategoricalQuantitativeOverlayConfig) overlayConfig;
      if (!colorConfig.getDenominatorValues().containsAll(colorConfig.getNumeratorValues())) {
        throw new IllegalArgumentException("CategoricalQuantitativeOverlay numerator values must be a subset of denominator values.");
      }
      variableValueQuantifier = raw -> (colorConfig.getNumeratorValues().contains(raw) ? 1.0 : colorConfig.getDenominatorValues().contains(raw) ? 0 : null);
      aggregatorSupplier = (index) -> new CategoricalProportionAggregator(new HashSet<>(colorConfig.getNumeratorValues()),
          new HashSet<>(colorConfig.getDenominatorValues()), index);
    } else {
      if (!varShapeFinder.apply(overlayVariable).equalsIgnoreCase(APIVariableDataShape.CONTINUOUS.getValue())) {
        throw new IllegalArgumentException("Incorrect overlay configuration type for continuous var: " + varShapeFinder.apply(overlayVariable));
      }
      aggregatorSupplier = i -> ContinuousAggregators.fromExternalString(((ContinuousQuantitativeOverlayConfig) overlayConfig).getAggregator().getValue())
          .getAggregatorSupplier(i);
    }
  }

  public VariableSpec getOverlayVariable() {
    return overlayVariable;
  }

  public MarkerAggregator<Double> getAggregatorProvider(int index) {
    return aggregatorSupplier.apply(index);
  }

  public Function<String, Double> getVariableValueQuantifier() {
    return variableValueQuantifier;
  }
}
