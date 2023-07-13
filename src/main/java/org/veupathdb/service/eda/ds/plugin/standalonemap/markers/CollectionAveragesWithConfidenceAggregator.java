package org.veupathdb.service.eda.ds.plugin.standalonemap.markers;

import org.veupathdb.service.eda.common.plugin.util.PluginUtil;
import org.veupathdb.service.eda.generated.model.VariableSpec;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class CollectionAveragesWithConfidenceAggregator implements MarkerAggregator<Map<String, AveragesWithConfidence>> {
  private final Function<Integer, String> variableIdByIndex;
  private final Map<String, AveragesWithConfidenceAggregator> averageAggregators = new HashMap<>();

  public CollectionAveragesWithConfidenceAggregator(Function<Integer, String> variableIdByIndex,
                                                    Function<String, Integer> indexByVarId,
                                                    List<String> variableIds) {
    this.variableIdByIndex = variableIdByIndex;
    variableIds.forEach(var -> averageAggregators.put(var, new AveragesWithConfidenceAggregator(indexByVarId.apply(var))));
  }

  @Override
  public void addValue(String[] d) {
    for (int i = 0; i < d.length; i++) {
      String var = variableIdByIndex.apply(i);
      AveragesWithConfidenceAggregator agg = averageAggregators.get(var);
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
}
