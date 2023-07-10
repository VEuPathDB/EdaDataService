package org.veupathdb.service.eda.ds.plugin.standalonemap.markers;

import org.veupathdb.service.eda.generated.model.APIVariableDataShape;
import org.veupathdb.service.eda.generated.model.CategoricalQuantitativeOverlayConfig;
import org.veupathdb.service.eda.generated.model.ContinuousQuantitativeOverlayConfig;
import org.veupathdb.service.eda.generated.model.QuantitativeOverlayConfig;
import org.veupathdb.service.eda.generated.model.VariableSpec;

import java.util.HashSet;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.veupathdb.service.eda.generated.model.OverlayType.CATEGORICAL;

public class MapBubbleSpecification {
  private final VariableSpec overlayVariable;
  private final Supplier<MarkerAggregator<Double>> aggregatorSupplier;

  /**
   * Constructs a map bubble specification from the raw input. Note that this will throw an IllegalArgumentException
   * with a user-friendly message if there are any user input errors.
   */
  public MapBubbleSpecification(QuantitativeOverlayConfig overlayConfig,
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
      aggregatorSupplier = () -> new CategoricalProportionAggregator(new HashSet<>(colorConfig.getNumeratorValues()),
          new HashSet<>(colorConfig.getDenominatorValues()));
    } else {
      if (!varShapeFinder.apply(overlayVariable).equalsIgnoreCase(APIVariableDataShape.CONTINUOUS.getValue())) {
        throw new IllegalArgumentException("Incorrect overlay configuration type for continuous var: " + varShapeFinder.apply(overlayVariable));
      }

      aggregatorSupplier = ContinuousAggregators.fromExternalString(((ContinuousQuantitativeOverlayConfig) overlayConfig).getAggregator().getValue())
          .getAggregatorSupplier();
    }
  }

  public VariableSpec getOverlayVariable() {
    return overlayVariable;
  }

  public Supplier<MarkerAggregator<Double>> getAggregatorSupplier() {
    return aggregatorSupplier;
  }
}
