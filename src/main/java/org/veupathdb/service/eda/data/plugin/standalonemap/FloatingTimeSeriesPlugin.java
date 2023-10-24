package org.veupathdb.service.eda.data.plugin.standalonemap;

import java.util.List;
import org.veupathdb.service.eda.common.plugin.constraint.ConstraintSpec;

import static org.veupathdb.service.eda.data.metadata.AppsMetadata.VECTORBASE_PROJECT;

public class FloatingTimeSeriesPlugin extends FloatingLineplotPlugin {

  @Override
  public String getDisplayName() {
    return "Time series";
  }

  @Override
  public String getDescription() {
    return "Visualize aggregate values of one variable across the sequential values of a temporal variable";
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
        .element("yAxisVariable")
          .description("Variable must be of the same or a child entity as the X-axis variable.")
        .element("xAxisVariable")
          .temporal(true)
          .description("Variable must be temporal and belong to the same or child entity as the variable the map markers are configured with, if any.")
        .element("overlayVariable")
          .required(false)
      .done();
  }
}
