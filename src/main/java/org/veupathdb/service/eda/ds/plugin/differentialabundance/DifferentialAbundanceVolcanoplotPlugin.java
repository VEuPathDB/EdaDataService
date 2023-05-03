package org.veupathdb.service.eda.ds.plugin.differentialabundance;

import org.gusdb.fgputil.validation.ValidationException;
import org.veupathdb.service.eda.common.client.spec.StreamSpec;
import org.veupathdb.service.eda.common.plugin.constraint.ConstraintSpec;
import org.veupathdb.service.eda.common.plugin.constraint.DataElementSet;
import org.veupathdb.service.eda.common.plugin.util.RServeClient;
import org.veupathdb.service.eda.ds.Resources;
import org.veupathdb.service.eda.ds.metadata.AppsMetadata;
import org.veupathdb.service.eda.ds.plugin.AbstractPlugin;
import org.veupathdb.service.eda.generated.model.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.veupathdb.service.eda.common.plugin.util.RServeClient.useRConnectionWithRemoteFiles;

public class DifferentialAbundanceVolcanoplotPlugin extends AbstractPlugin<DifferentialAbundanceVolcanoplotPostRequest, DifferentialAbundanceVolcanoplotSpec, DifferentialAbundanceComputeConfig> {
  
  @Override
  public String getDisplayName() {
    return "Volcano plot";
  }

  @Override
  public String getDescription() {
    return "Display fold change vs. significance for a differential abundance analysis.";
  }

  @Override
  public List<String> getProjects() {
    return List.of(AppsMetadata.MICROBIOME_PROJECT);
  }

  @Override
  protected ClassGroup getTypeParameterClasses() {
    return new ClassGroup(DifferentialAbundanceVolcanoplotPostRequest.class, DifferentialAbundanceVolcanoplotSpec.class, DifferentialAbundanceComputeConfig.class);
  }

  // // I think we can remove this whole thing ?!? there are no variables for the user to select
  // @Override
  // public ConstraintSpec getConstraintSpec() {
  //   return new ConstraintSpec()
  //     .dependencyOrder(List.of("yAxisVariable", "xAxisVariable"), List.of("overlayVariable"))
  //     .pattern()
  //       .element("overlayVariable")
  //         .required(false)
  //         .maxValues(8)
  //         .description("Variable must be a number, or have 8 or fewer values, and be of the same or a parent entity as the X-axis variable.")
  //     .pattern()
  //       .element("overlayVariable")
  //         .required(false)
  //         .types(APIVariableType.NUMBER, APIVariableType.INTEGER) 
  //         .description("Variable must be a number, or have 8 or fewer values, and be of the same or a parent entity as the X-axis variable.")
  //     .done();
  // }

  @Override
  protected void validateVisualizationSpec(DifferentialAbundanceVolcanoplotSpec pluginSpec) throws ValidationException {
    // nothing to do here
  }

  @Override
  protected List<StreamSpec> getRequestedStreams(DifferentialAbundanceVolcanoplotSpec pluginSpec) {
    // this plugin only uses the stats result of the compute; no tabular data streams needed
    return Collections.emptyList();
  }

  @Override
  protected void writeResults(OutputStream out, Map<String, InputStream> dataStreams) throws IOException {
    writeComputeStatsResponseToOutput(out);
  }
}
