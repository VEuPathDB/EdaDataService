package org.veupathdb.service.eda.data.plugin.standalonemap;

import java.util.List;
import org.veupathdb.service.eda.common.plugin.constraint.ConstraintSpec;

import static org.veupathdb.service.eda.data.metadata.AppsMetadata.VECTORBASE_PROJECT;

public class CollectionFloatingTimeSeriesPlugin extends CollectionFloatingLineplotPlugin {

  @Override
  public String getDisplayName() {
    return "Time series";
  }

  @Override
  public String getDescription() {
    return "Visualize aggregate values of one variable across the sequential values of a temporal Variable Group";
  }

  @Override
  public List<String> getProjects() {
    return List.of(VECTORBASE_PROJECT);
  }
  
  @Override
  public ConstraintSpec getConstraintSpec() {
    return new ConstraintSpec()
      .dependencyOrder(List.of("yAxisVariable"), List.of("xAxisVariable", "overlayVariable"))
      .pattern()
        .element("xAxisVariable")
          .temporal(true)
          .description("Variable must be temporal and belong to the same or child entity as the variable the map markers are configured with.")
      .done();
  }
}
