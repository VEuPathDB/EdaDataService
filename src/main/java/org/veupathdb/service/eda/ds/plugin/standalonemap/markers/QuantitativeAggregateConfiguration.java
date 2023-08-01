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
  private final Function<Integer, MarkerAggregator<AveragesWithConfidence>> averageWithConfAggregatorSupplier;

  private Function<String, Double> variableValueQuantifier;

  /**
   * Constructs a map bubble specification from the raw input. Note that this will throw an IllegalArgumentException
   * with a user-friendly message if there are any user input errors.
   */
  public QuantitativeAggregateConfiguration(QuantitativeAggregationConfig overlayConfig,
                                            String varShape) {
    if (CATEGORICAL.equals(overlayConfig.getOverlayType())) {
      if (varShape.equalsIgnoreCase(APIVariableDataShape.CONTINUOUS.getValue())) {
        throw new IllegalArgumentException("Incorrect overlay configuration type for categorical var: " + varShape);
      }
      CategoricalAggregationConfig categoricalConfig = (CategoricalAggregationConfig) overlayConfig;
      if (!categoricalConfig.getDenominatorValues().containsAll(categoricalConfig.getNumeratorValues())) {
        throw new IllegalArgumentException("CategoricalQuantitativeOverlay numerator values must be a subset of denominator values.");
      }
      variableValueQuantifier = Double::valueOf;
      aggregatorSupplier = (index) -> new CategoricalProportionAggregator(new HashSet<>(categoricalConfig.getNumeratorValues()),
          new HashSet<>(categoricalConfig.getDenominatorValues()), index);
      averageWithConfAggregatorSupplier = (index) -> new ProportionWithConfidenceAggregator(index,
          categoricalConfig.getNumeratorValues(),
          categoricalConfig.getDenominatorValues());
    } else {
      if (!varShape.equalsIgnoreCase(APIVariableDataShape.CONTINUOUS.getValue())) {
        throw new IllegalArgumentException("Incorrect overlay configuration type for continuous var: " + varShape);
      }
      ContinuousAggregators continuousAgg = ContinuousAggregators.fromExternalString(((ContinuousAggregationConfig) overlayConfig).getAggregator().getValue());
      variableValueQuantifier = Double::valueOf;
      averageWithConfAggregatorSupplier = continuousAgg::getAverageWithConfidenceAggregator;
      aggregatorSupplier = continuousAgg::getAggregatorSupplier;
    }
  }

  public MarkerAggregator<AveragesWithConfidence> getAverageWithConfidenceAggregatorProvider(int index) {
    return averageWithConfAggregatorSupplier.apply(index);
  }

  public MarkerAggregator<Double> getAverageAggregatorProvider(int index) {
    return aggregatorSupplier.apply(index);
  }

  public Function<String, Double> getVariableValueQuantifier() {
    return variableValueQuantifier;
  }
}
