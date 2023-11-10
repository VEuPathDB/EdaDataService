package org.veupathdb.service.eda.ds.plugin.correlationassaymetadata;

import org.gusdb.fgputil.json.JsonUtil;
import org.gusdb.fgputil.validation.ValidationException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.veupathdb.service.eda.common.client.spec.StreamSpec;
import org.veupathdb.service.eda.common.plugin.util.RServeClient;
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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    CorrelationAssayMetadataStatsResponse stats = getComputeResultStats(CorrelationAssayMetadataStatsResponse.class);

    // Don't even use R at all?
    // Print out stats as they are
    JSONObject bipartiteNetwork = new JSONObject();
    JSONArray links = new JSONArray();
    ArrayList<String> nodeIDs = new ArrayList<>();

    // Create the column ids. Just need the unique values from data1, then from data2
    ArrayList<String> column1NodeIDs = new ArrayList<>();
    ArrayList<String> column2NodeIDs = new ArrayList<>();

    // Just loop through stats and add to col ids, links, etc. That'll be way easier. Then at the end union ids to get nodes. Voila!
    stats.getStatistics().forEach((point) -> {

      // Skip links that have a correlation coeff less than 0.7
      // if (point.getCorrelationCoef() == null) return;
      
      JSONObject sourceNode = new JSONObject();
      sourceNode.put("id", point.getData1());
      JSONObject targetNode = new JSONObject();
      targetNode.put("id", point.getData2());
      
      nodeIDs.add(point.getData1());
      nodeIDs.add(point.getData2());
      
      column1NodeIDs.add(point.getData1());
      column2NodeIDs.add(point.getData2());
      
      if (point.getCorrelationCoef() == null) return;
      if (Math.abs(Float.parseFloat(point.getCorrelationCoef())) < 0.2) return;

      JSONObject link = new JSONObject();
      link.put("source", sourceNode);
      link.put("target", targetNode);
      link.put("strokeWidth", point.getCorrelationCoef());
      // Link color is the sign of the correlation
      String color = Float.parseFloat(point.getCorrelationCoef()) < 0 ? "-1" : "1";
      link.put("color", color);
      links.put(link);
    });


    List<String> uniqueColumn1IDs = column1NodeIDs.stream().distinct().collect(Collectors.toList());
    List<String> uniqueColumn2IDs = column2NodeIDs.stream().distinct().collect(Collectors.toList());

    List<String> uniqueNodeIDs = nodeIDs.stream().distinct().collect(Collectors.toList());
    System.out.println(nodeIDs);
    System.out.println(uniqueNodeIDs);

    JSONArray nodes = new JSONArray();
    uniqueNodeIDs.forEach((nodeID) -> {
      JSONObject node = new JSONObject();
      node.put("id", nodeID);
      nodes.put(node);
    });

    bipartiteNetwork.put("nodes", nodes);
    bipartiteNetwork.put("links", links);
    bipartiteNetwork.put("column1NodeIDs", uniqueColumn1IDs);
    bipartiteNetwork.put("column2NodeIDs", uniqueColumn2IDs);

    System.out.println(bipartiteNetwork.toString());

    out.write(bipartiteNetwork
        .toString()
        .getBytes(StandardCharsets.UTF_8)
      );


    
    
    // useRConnectionWithRemoteFiles(Resources.RSERVE_URL, dataStreams, connection -> {
  
    //   // TEMPORARY - will be removed after plot.data #234 is complete
    //   // Take the stats response object and write it into R
    //   // as a data.frame. There's probably a much more elegant solution, please advise!
    //   connection.voidEval("statsDf <- data.frame(correlationCoef = character(), data1 = character(), data2 = character(), stringsAsFactors=FALSE)");
    //   for (int i = 0; i < stats.getStatistics().toArray().length; i++) {
    //       CorrelationPoint point = stats.getStatistics().get(i);
    //       String newRow = "list(correlationCoef='" + point.getCorrelationCoef() + "', data1='" + point.getData1() + "', data2='" + point.getData2() + "')";
    //       connection.voidEval("newRow <- " + newRow);
    //       connection.voidEval("statsDf <- rbind(statsDf, newRow)");
        
    //     }
        
    //   // The following will be revived later when we have improved the plot.data network classes (plot.data #234)
    //   // connection.voidEval("bpNet <- plot.data::bipartiteNetwork(" +
    //   //                                                       "df=statsDf," +
    //   //                                                       "sourceNodeColumn='data1'," +
    //   //                                                       "targetNodeColumn='data2'," + 
    //   //                                                       "linkWeightColumn='correlationCoef'," +
    //   //                                                       "linkColorScheme='posneg'," +
    //   //                                                       "verbose=TRUE)");

    //   // String command = "plot.data::writeNetworkToJSON(bpNet, pattern='bipartiteNetwork', verbose=TRUE)";
    //   // RServeClient.streamResult(connection, command, out);
    // });
  }
}
