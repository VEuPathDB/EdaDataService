package org.veupathdb.service.eda.ds.plugin.standalonemap.markers;

import org.veupathdb.service.eda.generated.model.APIVariableDataShape;
import org.veupathdb.service.eda.generated.model.CategoricalColorConfig;
import org.veupathdb.service.eda.generated.model.ColorConfig;
import org.veupathdb.service.eda.generated.model.ContinousColorConfig;
import org.veupathdb.service.eda.generated.model.VariableSpec;

import java.util.HashSet;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.veupathdb.service.eda.generated.model.OverlayType.CATEGORICAL;

public class MapBubbleSpecification {
  private final VariableSpec overlayVariable;
  private OverlayRecoder overlayRecoder;
  private Supplier<MarkerAggregator<Double>> aggregatorSupplier;

  public MapBubbleSpecification(ColorConfig overlayConfig,
                                Function<VariableSpec, String> varShapeFinder) {
    this.overlayVariable = overlayConfig.getOverlayVariable();
    if (CATEGORICAL.equals(overlayConfig.getOverlayType())) {

      if (!varShapeFinder.apply(overlayVariable).equalsIgnoreCase(APIVariableDataShape.CATEGORICAL.getValue())) {
        throw new IllegalArgumentException("Incorrect overlay configuration type for categorical var: " + varShapeFinder.apply(overlayVariable));
      }

      CategoricalColorConfig colorConfig = (CategoricalColorConfig) overlayConfig;
      aggregatorSupplier = () -> new CategoricalRatioAggregator(new HashSet<>(colorConfig.getNumeratorValues()),
          new HashSet<>(colorConfig.getNumeratorValues()));
    } else {

      if (!varShapeFinder.apply(overlayVariable).equalsIgnoreCase(APIVariableDataShape.CONTINUOUS.getValue())) {
        throw new IllegalArgumentException("Incorrect overlay configuration type for continuous var: " + varShapeFinder.apply(overlayVariable));
      }

      aggregatorSupplier = ContinuousAggregators.fromExternalString(((ContinousColorConfig) overlayConfig).getAggregator()).getAggregatorSupplier();
    }
  }

  public VariableSpec getOverlayVariable() {
    return overlayVariable;
  }

  public Supplier<MarkerAggregator<Double>> getAggregatorSupplier() {
    return aggregatorSupplier;
  }
}
