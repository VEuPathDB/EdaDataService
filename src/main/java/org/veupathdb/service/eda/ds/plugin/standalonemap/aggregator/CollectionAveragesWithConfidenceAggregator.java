package org.veupathdb.service.eda.ds.plugin.standalonemap.aggregator;

import org.veupathdb.service.eda.ds.plugin.standalonemap.markers.QuantitativeAggregateConfiguration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Produces a collection of aggregations for variable values.
 */
public class CollectionAveragesWithConfidenceAggregator implements MarkerAggregator<Map<String, AveragesWithConfidence>> {
  private final Function<Integer, String> variableIdByIndex;
  private final Map<String, MarkerAggregator<AveragesWithConfidence>> averageAggregators = new HashMap<>();

  /**
   *
   * @param variableIdByIndex Mapping function to find a variable ID in dot notation based on tabular index.
   * @param indexByVarId Mapping function to find index from a variable ID in dot notation.
   * @param variableIds Collection variables in dot-notation to aggregate the values of.
   */
  public CollectionAveragesWithConfidenceAggregator(Function<Integer, String> variableIdByIndex,
                                                    Function<String, Integer> indexByVarId,
                                                    List<String> variableIds,
                                                    QuantitativeAggregateConfiguration configuration) {
    this.variableIdByIndex = variableIdByIndex;
    // Create new aggregators for each collection variable.
    variableIds.forEach(var -> averageAggregators.put(var,
        configuration.getAverageWithConfidenceAggregatorProvider(indexByVarId.apply(var))));
  }

  @Override
  public void addValue(String[] d) {
    for (int i = 0; i < d.length; i++) {
      String var = variableIdByIndex.apply(i);
      MarkerAggregator<AveragesWithConfidence> agg = averageAggregators.get(var);
      if (agg != null) {
        agg.addValue(d);
      }
    }
  }

  @Override
  public Map<String, AveragesWithConfidence> finish() {
    return averageAggregators.entrySet().stream()
        .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().finish()));
  }

  @Override
  public boolean appliesTo(String[] rec) {
    return averageAggregators.entrySet().stream().anyMatch(a -> a.getValue().appliesTo(rec));
  }
}
