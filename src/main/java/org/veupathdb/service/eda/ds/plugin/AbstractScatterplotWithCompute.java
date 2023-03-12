package org.veupathdb.service.eda.ds.plugin;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.veupathdb.service.eda.common.plugin.constraint.ConstraintSpec;
import org.veupathdb.service.eda.ds.metadata.AppsMetadata;
import org.veupathdb.service.eda.generated.model.APIVariableType;
import org.veupathdb.service.eda.generated.model.VisualizationRequestBase;

import java.util.List;

public abstract class AbstractScatterplotWithCompute<T extends VisualizationRequestBase,S,R> extends AbstractPlugin<T, S, R> {

  private static final Logger LOG = LogManager.getLogger(AbstractScatterplotWithCompute.class);

  @Override
  public String getDisplayName() {
    return "Scatter plot";
  }

  @Override
  public List<String> getProjects() {
    return List.of(AppsMetadata.MICROBIOME_PROJECT);
  }

  /**
   * Two variable patterns supported: xAxis and facet variables' constraints are the same in each; overlay has two
   * forms: number, or non-number with <=8 distinct values
   *
   * @return constraints for this plugin's variable inputs
   */
  @Override
  public ConstraintSpec getConstraintSpec() {
    return new ConstraintSpec()
      .dependencyOrder(List.of("xAxisVariable"), List.of("overlayVariable", "facetVariable"))
      .pattern()
        .element("xAxisVariable")
          .types(APIVariableType.NUMBER, APIVariableType.DATE, APIVariableType.INTEGER)
          .description("Variable must be a number or date and be of the same or a parent entity as the Y-axis variable.")
        .element("facetVariable")
          .required(false)
          .maxVars(2)
          .maxValues(10)
          .description("Variable(s) must have 10 or fewer unique values and be of the same or a parent entity as the Overlay variable.")
        .element("overlayVariable")
          .required(false)
          .maxValues(8)
          .description("Variable must be a number, or have 8 or fewer values, and be of the same or a parent entity as the X-axis variable.")
      .pattern()
        .element("xAxisVariable")
          .types(APIVariableType.NUMBER, APIVariableType.DATE, APIVariableType.INTEGER)
          .description("Variable must be a number or date and be of the same or a parent entity as the Y-axis variable.")
        .element("facetVariable")
          .required(false)
          .maxVars(2)
          .maxValues(10)
          .description("Variable(s) must have 10 or fewer unique values and be of the same or a parent entity as the Overlay variable.")
        .element("overlayVariable")
          .required(false)
          .types(APIVariableType.NUMBER, APIVariableType.INTEGER)
          .description("Variable must be a number, or have 8 or fewer values, and be of the same or a parent entity as the X-axis variable.")
      .done();
  }
}
