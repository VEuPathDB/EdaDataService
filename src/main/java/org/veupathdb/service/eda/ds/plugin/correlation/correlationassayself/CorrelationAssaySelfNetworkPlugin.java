package org.veupathdb.service.eda.ds.plugin.correlation.correlationassayself;

import org.gusdb.fgputil.validation.ValidationException;
import org.veupathdb.service.eda.common.client.spec.StreamSpec;
import org.veupathdb.service.eda.common.plugin.constraint.ConstraintSpec;
import org.veupathdb.service.eda.common.plugin.constraint.DataElementSet;
import org.veupathdb.service.eda.common.plugin.util.RServeClient;
import org.veupathdb.service.eda.ds.Resources;
import org.veupathdb.service.eda.ds.metadata.AppsMetadata;
import org.veupathdb.service.eda.ds.core.AbstractPlugin;
import org.veupathdb.service.eda.generated.model.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.veupathdb.service.eda.common.plugin.util.RServeClient.useRConnectionWithRemoteFiles;

public class CorrelationAssaySelfNetworkPlugin extends AbstractPlugin<CorrelationAssaySelfPostRequest, CorrelationNetworkSpec, CorrelationAssaySelfConfig> {
  
  @Override
  public String getDisplayName() {
    return "Network";
  }

  @Override
  public String getDescription() {
    return "Visualize the correlation between variables in a data set";
  }

  @Override
  public List<String> getProjects() {
    return List.of(AppsMetadata.MICROBIOME_PROJECT);
  }

  @Override
  protected ClassGroup getTypeParameterClasses() {
    return new ClassGroup(CorrelationAssaySelfPostRequest.class, CorrelationNetworkSpec.class, CorrelationAssaySelfConfig.class);
  }

  @Override
  public boolean computeGeneratesVars() {
    return false;
  }

  @Override
  protected void validateVisualizationSpec(CorrelationBipartitenetworkSpec pluginSpec) throws ValidationException {
    // nothing to do here
  }

  @Override
  protected List<StreamSpec> getRequestedStreams(CorrelationBipartitenetworkSpec pluginSpec) {
    // this plugin only uses the stats result of the compute; no tabular data streams needed
    return Collections.emptyList();
  }

  @Override
  protected void writeResults(OutputStream out, Map<String, InputStream> dataStreams) throws IOException {
    CorrelationNetworkSpec spec = getPluginSpec();
    String layout = spec.getLayout() != null ? ", layout = '" + spec.getLayout().getValue() + "'" : "";
    ByteArrayOutputStream statsBytes = new ByteArrayOutputStream();
    writeComputeStatsResponseToOutput(out);
    ByteArrayInputStream statsIn = new ByteArrayInputStream(statsBytes.toByteArray());
    dataStreams.put("stats-file.tab", statsIn);

    useRConnectionWithRemoteFiles(Resources.RSERVE_URL, dataStreams, connection -> {
      String command = "veupathUtils::writeJSON(plot.data::Network(data.table::fread(stats-file.tab)" + layout + "))";
      RServeClient.streamResult(connection, command, out);
    }); 
  }
}
