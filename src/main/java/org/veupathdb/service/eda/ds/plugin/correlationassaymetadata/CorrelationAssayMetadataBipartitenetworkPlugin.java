package org.veupathdb.service.eda.ds.plugin.correlationassaymetadata;

import org.gusdb.fgputil.validation.ValidationException;
import org.veupathdb.service.eda.common.client.spec.StreamSpec;
import org.veupathdb.service.eda.ds.metadata.AppsMetadata;
import org.veupathdb.service.eda.ds.core.AbstractPlugin;
import org.veupathdb.service.eda.generated.model.BoxplotPostRequest;
import org.veupathdb.service.eda.generated.model.BoxplotSpec;
// import org.veupathdb.service.eda.generated.model.CorrelationAssayMetadataComputeConfig;
import org.veupathdb.service.eda.generated.model.CorrelationAssayMetadataBipartitenetworkPostRequest;
import org.veupathdb.service.eda.generated.model.EmptyDataPluginSpec;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class CorrelationAssayMetadataBipartitenetworkPlugin extends AbstractPlugin<CorrelationAssayMetadataBipartitenetworkPostRequest, EmptyDataPluginSpec, EmptyDataPluginSpec> {

  @Override
  public String getDisplayName() {
    return "Bipartite network";
  }

  @Override
  public String getDescription() {
    return "Visualize the correlation between two sets of data";
  }

  @Override
  public List<String> getProjects() {
    return List.of(AppsMetadata.MICROBIOME_PROJECT);
  }

  @Override
  protected ClassGroup getTypeParameterClasses() {
    return new ClassGroup(CorrelationAssayMetadataBipartitenetworkPostRequest.class, EmptyDataPluginSpec.class, EmptyDataPluginSpec.class);
  }

  @Override
  protected boolean computeGeneratesVars() {
    return false;
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
    System.out.println("I'm writing results!");
    // writeComputeStatsResponseToOutput(out);
  }
}
