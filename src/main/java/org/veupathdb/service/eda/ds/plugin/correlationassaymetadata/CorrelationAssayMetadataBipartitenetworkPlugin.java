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

    // TEMPORARY: Reshape data using java instead of calling out to R+plot.data
    // The goal is to transform the response from the correlation app (CorrelationAssayMetadataStatsResponse)
    // into a BipartiteNetwork that can be sent to the frontend. The BipartiteNetwork is composed
    // of nodes, links, column1NodeIDs, and column2NodeIDs.

    // Prep objects
    JSONObject bipartiteNetwork = new JSONObject();
    JSONArray links = new JSONArray();
    ArrayList<String> nodeIDs = new ArrayList<>();
    ArrayList<String> column1NodeIDs = new ArrayList<>();
    ArrayList<String> column2NodeIDs = new ArrayList<>();

    // All the information we need is contained in the stats object. Each row of stats corresponds to a link.
    // We want to grab the link, the nodes associated with the link, and make note of column assignments for
    // each node.
    stats.getStatistics().forEach((correlationRow) -> {

      // First add the node ids (data1 and data2 from this row) to our growing list of node ids
      // We'll worry about duplicates later.
      nodeIDs.add(correlationRow.getData1());
      nodeIDs.add(correlationRow.getData2());

      // Add the data1 ids to column 1, and data2 ids to column 2. Again we'll address duplicates later.
      column1NodeIDs.add(correlationRow.getData1());
      column2NodeIDs.add(correlationRow.getData2());

      // Next create links
      // Skip rows that have no correlation coefficient or a correlation coef that is too small
      if (correlationRow.getCorrelationCoef() == null) return;
      if (Math.abs(Float.parseFloat(correlationRow.getCorrelationCoef())) < 0.2) return;

      // Create source and target objects.
      JSONObject sourceNode = new JSONObject();
      sourceNode.put("id", correlationRow.getData1());
      JSONObject targetNode = new JSONObject();
      targetNode.put("id", correlationRow.getData2());
      
      // Create link with the data from this row and add to links array.
      JSONObject link = new JSONObject();
      link.put("source", sourceNode);
      link.put("target", targetNode);
      link.put("strokeWidth", correlationRow.getCorrelationCoef());
      // Link color is the sign of the correlation
      String color = Float.parseFloat(correlationRow.getCorrelationCoef()) < 0 ? "-1" : "1";
      link.put("color", color);
      links.put(link);
    });

    // Get unique IDs
    List<String> uniqueColumn1IDs = column1NodeIDs.stream().distinct().collect(Collectors.toList());
    List<String> uniqueColumn2IDs = column2NodeIDs.stream().distinct().collect(Collectors.toList());
    List<String> uniqueNodeIDs = nodeIDs.stream().distinct().collect(Collectors.toList());

    // Turn the node ids into an array of Node objects, each with an id property
    JSONArray nodes = new JSONArray();
    uniqueNodeIDs.forEach((nodeID) -> {
      JSONObject node = new JSONObject();
      node.put("id", nodeID);
      nodes.put(node);
    });

    // Finally create the bipartite network and send it off!
    bipartiteNetwork.put("nodes", nodes);
    bipartiteNetwork.put("links", links);
    bipartiteNetwork.put("column1NodeIDs", uniqueColumn1IDs);
    bipartiteNetwork.put("column2NodeIDs", uniqueColumn2IDs);

    out.write(bipartiteNetwork
        .toString()
        .getBytes(StandardCharsets.UTF_8)
      );

  }
}
