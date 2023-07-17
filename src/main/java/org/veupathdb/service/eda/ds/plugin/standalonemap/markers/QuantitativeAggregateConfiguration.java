package org.veupathdb.service.eda.ds.plugin.standalonemap.markers;

import org.veupathdb.service.eda.generated.model.APIVariableDataShape;
import org.veupathdb.service.eda.generated.model.CategoricalAggregationConfig;
import org.veupathdb.service.eda.generated.model.ContinuousAggregationConfig;
import org.veupathdb.service.eda.generated.model.QuantitativeAggregationConfig;

import java.util.HashSet;
import java.util.function.Function;

import static org.veupathdb.service.eda.generated.model.OverlayType.CATEGORICAL;

public class QuantitativeAggregateConfiguration {
  private final Function<Integer, MarkerAggregator<Double>> aggregatorSupplier;
  private Function<String, Double> variableValueQuantifier;

  /**
   * Constructs a map bubble specification from the raw input. Note that this will throw an IllegalArgumentException
   * with a user-friendly message if there are any user input errors.
   */
  public QuantitativeAggregateConfiguration(QuantitativeAggregationConfig overlayConfig,
                                            String varShape) {
    if (CATEGORICAL.equals(overlayConfig.getOverlayType())) {
      if (!varShape.equalsIgnoreCase(APIVariableDataShape.CATEGORICAL.getValue())
          && !varShape.equalsIgnoreCase(APIVariableDataShape.ORDINAL.getValue())) {
        throw new IllegalArgumentException("Incorrect overlay configuration type for categorical var: " + varShape);
      }
      CategoricalAggregationConfig colorConfig = (CategoricalAggregationConfig) overlayConfig;
      if (!colorConfig.getDenominatorValues().containsAll(colorConfig.getNumeratorValues())) {
        throw new IllegalArgumentException("CategoricalQuantitativeOverlay numerator values must be a subset of denominator values.");
      }
      variableValueQuantifier = raw -> (colorConfig.getNumeratorValues().contains(raw) ? 1.0 : colorConfig.getDenominatorValues().contains(raw) ? 0 : null);
      aggregatorSupplier = (index) -> new CategoricalProportionAggregator(new HashSet<>(colorConfig.getNumeratorValues()),
          new HashSet<>(colorConfig.getDenominatorValues()), index);
    } else {
      if (!varShape.equalsIgnoreCase(APIVariableDataShape.CONTINUOUS.getValue())) {
        throw new IllegalArgumentException("Incorrect overlay configuration type for continuous var: " + varShape);
      }
      aggregatorSupplier = i -> ContinuousAggregators.fromExternalString(((ContinuousAggregationConfig) overlayConfig).getAggregator().getValue())
          .getAggregatorSupplier(i);
    }
  }

  public MarkerAggregator<Double> getAggregatorProvider(int index) {
    return aggregatorSupplier.apply(index);
  }

  public Function<String, Double> getVariableValueQuantifier() {
    return variableValueQuantifier;
  }
}
