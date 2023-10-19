package org.veupathdb.service.eda.ds.plugin.standalonemap.markers;

import org.veupathdb.service.eda.ds.plugin.standalonemap.aggregator.AveragesWithConfidence;
import org.veupathdb.service.eda.ds.plugin.standalonemap.aggregator.CategoricalProportionAggregator;
import org.veupathdb.service.eda.ds.plugin.standalonemap.aggregator.ContinuousAggregators;
import org.veupathdb.service.eda.ds.plugin.standalonemap.aggregator.MarkerAggregator;
import org.veupathdb.service.eda.ds.plugin.standalonemap.aggregator.ProportionWithConfidenceAggregator;
import org.veupathdb.service.eda.generated.model.APIVariableDataShape;
import org.veupathdb.service.eda.generated.model.APIVariableType;
import org.veupathdb.service.eda.generated.model.CategoricalAggregationConfig;
import org.veupathdb.service.eda.generated.model.ContinuousAggregationConfig;
import org.veupathdb.service.eda.generated.model.QuantitativeAggregationConfig;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.function.Function;

import static org.veupathdb.service.eda.generated.model.OverlayType.CATEGORICAL;

public class QuantitativeAggregateConfiguration {
  private final ContinuousAggregators.ContinuousAggregatorFactory aggregatorSupplier;

  private Function<String, Double> variableValueQuantifier;
  private String variableType;

  /**
   * Constructs a map bubble specification from the raw input. Note that this will throw an IllegalArgumentException
   * with a user-friendly message if there are any user input errors.
   */
  public QuantitativeAggregateConfiguration(QuantitativeAggregationConfig overlayConfig,
                                            String varShape,
                                            String variableType,
                                            List<String> overlayVocab) {
      this.variableType = variableType;
    if (CATEGORICAL.equals(overlayConfig.getOverlayType())) {
      if (varShape.equalsIgnoreCase(APIVariableDataShape.CONTINUOUS.getValue())) {
        throw new IllegalArgumentException("Incorrect overlay configuration type for categorical var: " + varShape);
      }
      CategoricalAggregationConfig categoricalConfig = (CategoricalAggregationConfig) overlayConfig;
      if (!categoricalConfig.getDenominatorValues().containsAll(categoricalConfig.getNumeratorValues())) {
        throw new IllegalArgumentException("CategoricalQuantitativeOverlay numerator values must be a subset of denominator values.");
      }
      variableValueQuantifier = Double::valueOf;
      aggregatorSupplier = new ContinuousAggregators.ContinuousAggregatorFactory() {
        @Override
        public MarkerAggregator<Double> create(int index, Function<String, Double> valueQuantifier) {
          return new CategoricalProportionAggregator(new HashSet<>(categoricalConfig.getNumeratorValues()),
              new HashSet<>(categoricalConfig.getDenominatorValues()), index);
        }

        @Override
        public MarkerAggregator<AveragesWithConfidence> createWithConfidence(int index, Function<String, Double> valueQuantifier) {
         return new ProportionWithConfidenceAggregator(index,
              categoricalConfig.getNumeratorValues(),
              categoricalConfig.getDenominatorValues(),
              overlayVocab);
        }
      };
    } else {
      if (!varShape.equalsIgnoreCase(APIVariableDataShape.CONTINUOUS.getValue())) {
        throw new IllegalArgumentException("Incorrect overlay configuration type for continuous var: " + varShape);
      }
      ContinuousAggregators continuousAgg = ContinuousAggregators.fromExternalString(((ContinuousAggregationConfig) overlayConfig).getAggregator().getValue());
      if (variableType.equalsIgnoreCase(APIVariableType.DATE.getValue())) {
        variableValueQuantifier = s -> (double) LocalDateTime.parse(s).toInstant(ZoneOffset.UTC).toEpochMilli();
      } else {
        variableValueQuantifier = Double::valueOf;
      }
      aggregatorSupplier = continuousAgg.getAggregatorFactory();
    }
  }

  public MarkerAggregator<AveragesWithConfidence> getAverageWithConfidenceAggregatorProvider(int index) {
    return aggregatorSupplier.createWithConfidence(index, variableValueQuantifier);
  }

  public MarkerAggregator<Double> getAverageAggregatorProvider(int index) {
    return aggregatorSupplier.create(index, variableValueQuantifier);
  }

  public String serializeAverage(double d) {
    if (variableType.equalsIgnoreCase(APIVariableType.DATE.getValue())) {
      return Instant.ofEpochMilli((long) d).toString();
    } else {
      return Double.toString(d);
    }
  }
}
