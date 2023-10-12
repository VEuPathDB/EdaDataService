package org.veupathdb.service.eda.ds.plugin.correlationassaymetadata;

import org.gusdb.fgputil.validation.ValidationException;
import org.veupathdb.service.eda.common.client.spec.StreamSpec;
import org.veupathdb.service.eda.ds.metadata.AppsMetadata;
import org.veupathdb.service.eda.ds.Resources;
import org.veupathdb.service.eda.ds.core.AbstractPlugin;
import org.veupathdb.service.eda.generated.model.CorrelationAssayMetadataBipartitenetworkPostRequest;
import org.veupathdb.service.eda.generated.model.CorrelationAssayMetadataBipartitenetworkSpec;
import org.veupathdb.service.eda.generated.model.CorrelationComputeConfig;
import org.veupathdb.service.eda.generated.model.CorrelationPoint;
import org.veupathdb.service.eda.generated.model.CorrelationAssayMetadataStatsResponse;
import org.veupathdb.service.eda.generated.model.ExamplePluginStats;

import static org.veupathdb.service.eda.common.plugin.util.RServeClient.useRConnectionWithRemoteFiles;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class CorrelationAssayMetadataBipartitenetworkPlugin extends AbstractPlugin<CorrelationAssayMetadataBipartitenetworkPostRequest, CorrelationAssayMetadataBipartitenetworkSpec, CorrelationComputeConfig> {

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
    return new ClassGroup(CorrelationAssayMetadataBipartitenetworkPostRequest.class, CorrelationAssayMetadataBipartitenetworkSpec.class, CorrelationComputeConfig.class);
  }

  @Override
  protected boolean computeGeneratesVars() {
    return false;
  }

  @Override
  protected void validateVisualizationSpec(CorrelationAssayMetadataBipartitenetworkSpec pluginSpec) throws ValidationException {
    // nothing to do here
  }

  @Override
  protected List<StreamSpec> getRequestedStreams(CorrelationAssayMetadataBipartitenetworkSpec pluginSpec) {
    // this plugin only uses the stats result of the compute; no tabular data streams needed
    return Collections.emptyList();
  }

  @Override
  protected void writeResults(OutputStream out, Map<String, InputStream> dataStreams) throws IOException {
    System.out.println("I'm writing results!");
    CorrelationAssayMetadataStatsResponse stats = getComputeResultStats(CorrelationAssayMetadataStatsResponse.class);


    
    useRConnectionWithRemoteFiles(Resources.RSERVE_URL, dataStreams, connection -> {
      connection.voidEval("print('im in R')");
      
      // TEMP there's probably a much more elegant solution, please advise!
      connection.voidEval("statsDf <- data.frame(correlationCoef = character(), data1 = character(), data2 = character(), stringsAsFactors=FALSE)");
      for (int i = 0; i < stats.getStatistics().toArray().length; i++) {
        CorrelationPoint point = stats.getStatistics().get(i);
        String newRow = "list(correlationCoef='" + point.getCorrelationCoef() + "', data1='" + point.getData1() + "', data2='" + point.getData2() + "')";
        connection.voidEval("newRow <- " + newRow);
        connection.voidEval("statsDf <- rbind(statsDf, newRow)");
        System.out.println(newRow);
        
      }
      connection.voidEval("print(head(statsDf))");
    });
  }
}
