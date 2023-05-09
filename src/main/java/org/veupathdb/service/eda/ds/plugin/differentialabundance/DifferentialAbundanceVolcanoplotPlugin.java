package org.veupathdb.service.eda.ds.plugin.differentialabundance;

import org.gusdb.fgputil.validation.ValidationException;
import org.veupathdb.service.eda.common.client.spec.StreamSpec;
import org.veupathdb.service.eda.ds.metadata.AppsMetadata;
import org.veupathdb.service.eda.ds.plugin.AbstractPlugin;
import org.veupathdb.service.eda.generated.model.DifferentialAbundanceComputeConfig;
import org.veupathdb.service.eda.generated.model.DifferentialAbundanceVolcanoplotPostRequest;
import org.veupathdb.service.eda.generated.model.EmptyDataPluginSpec;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class DifferentialAbundanceVolcanoplotPlugin extends AbstractPlugin<DifferentialAbundanceVolcanoplotPostRequest, EmptyDataPluginSpec, DifferentialAbundanceComputeConfig> {

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
    return new ClassGroup(DifferentialAbundanceVolcanoplotPostRequest.class, EmptyDataPluginSpec.class, DifferentialAbundanceComputeConfig.class);
  }

  @Override
  protected void validateVisualizationSpec(EmptyDataPluginSpec pluginSpec) throws ValidationException {
    // nothing to do here
  }

  @Override
  protected List<StreamSpec> getRequestedStreams(EmptyDataPluginSpec pluginSpec) {
    // this plugin only uses the stats result of the compute; no tabular data streams needed
    return Collections.emptyList();
  }

  @Override
  protected void writeResults(OutputStream out, Map<String, InputStream> dataStreams) throws IOException {
    writeComputeStatsResponseToOutput(out);
  }
}
