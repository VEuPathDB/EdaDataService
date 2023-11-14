package org.veupathdb.service.eda.ds.plugin.correlationassaymetadata;

import org.gusdb.fgputil.json.JsonUtil;
import org.gusdb.fgputil.validation.ValidationException;
import org.veupathdb.service.eda.common.client.spec.StreamSpec;
import org.veupathdb.service.eda.ds.metadata.AppsMetadata;
import org.veupathdb.service.eda.ds.core.AbstractPlugin;
import org.veupathdb.service.eda.generated.model.BipartiteNetwork;
import org.veupathdb.service.eda.generated.model.BipartiteNetworkConfig;
import org.veupathdb.service.eda.generated.model.BipartiteNetworkConfigImpl;
import org.veupathdb.service.eda.generated.model.BipartiteNetworkData;
import org.veupathdb.service.eda.generated.model.BipartiteNetworkDataImpl;
import org.veupathdb.service.eda.generated.model.BipartiteNetworkImpl;
import org.veupathdb.service.eda.generated.model.BipartiteNetworkPostResponse;
import org.veupathdb.service.eda.generated.model.BipartiteNetworkPostResponseImpl;
import org.veupathdb.service.eda.generated.model.CorrelationBipartitenetworkPostRequest;
import org.veupathdb.service.eda.generated.model.CorrelationBipartitenetworkSpec;
import org.veupathdb.service.eda.generated.model.CorrelationComputeConfig;
import org.veupathdb.service.eda.generated.model.CorrelationStatsResponse;
import org.veupathdb.service.eda.generated.model.LinkData;
import org.veupathdb.service.eda.generated.model.LinkDataImpl;
import org.veupathdb.service.eda.generated.model.NodeData;
import org.veupathdb.service.eda.generated.model.NodeDataImpl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CorrelationAssayMetadataBipartitenetworkPlugin extends AbstractPlugin<CorrelationBipartitenetworkPostRequest, CorrelationBipartitenetworkSpec, CorrelationComputeConfig> {

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
    return new ClassGroup(CorrelationBipartitenetworkPostRequest.class, CorrelationBipartitenetworkSpec.class, CorrelationComputeConfig.class);
  }

  @Override
  protected boolean computeGeneratesVars() {
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

    CorrelationStatsResponse stats = getComputeResultStats(CorrelationStatsResponse.class);
    Number correlationCoefThreshold = getPluginSpec().getCorrelationCoefThreshold() != null ? getPluginSpec().getCorrelationCoefThreshold() : 0.2;


    // TEMPORARY: Reshape data using java instead of calling out to R+plot.data
    // The goal is to transform the response from the correlation app (CorrelationStatsResponse)
    // into a BipartiteNetwork that can be sent to the frontend. The BipartiteNetwork is composed
    // of nodes, links, column1NodeIDs, and column2NodeIDs.

    // Prep objects
    ArrayList<LinkData> links = new ArrayList<LinkData>();
    ArrayList<String> nodeIDs = new ArrayList<>();
    ArrayList<String> column1NodeIDs = new ArrayList<>();
    ArrayList<String> column2NodeIDs = new ArrayList<>();

    // All the information we need is contained in the stats object. Each row of stats corresponds to a link.
    // We want to grab the link, the nodes associated with the link, and make note of column assignments for
    // each node.
    stats.getStatistics().forEach((correlationRow) -> {
      
      // Skip rows that have no correlation coefficient or a correlation coef that is too small. Filtering here prevents us
      // from showing nodes with no links.
      if (correlationRow.getCorrelationCoef() == null) return;
      if (Math.abs(Float.parseFloat(correlationRow.getCorrelationCoef())) < correlationCoefThreshold.floatValue()) return;

      // First add the node ids (data1 and data2 from this row) to our growing list of node ids
      // We'll worry about duplicates later.
      nodeIDs.add(correlationRow.getData1());
      nodeIDs.add(correlationRow.getData2());

      // Add the data1 ids to column 1, and data2 ids to column 2. Again we'll address duplicates later.
      column1NodeIDs.add(correlationRow.getData1());
      column2NodeIDs.add(correlationRow.getData2());

      // Next create links
      // Create source and target objects.
      NodeData sourceNode = new NodeDataImpl();
      sourceNode.setId(correlationRow.getData1());
      NodeData targetNode = new NodeDataImpl();
      targetNode.setId(correlationRow.getData2());
      
      // Create link with the data from this row and add to links array.
      LinkData link = new LinkDataImpl();
      link.setSource(sourceNode);
      link.setTarget(targetNode);
      link.setStrokeWidth(correlationRow.getCorrelationCoef());
      // Link color is the sign of the correlation
      String color = Float.parseFloat(correlationRow.getCorrelationCoef()) < 0 ? "-1" : "1";
      link.setColor(color);
      links.add(link);
    });

    // Get unique IDs
    List<String> uniqueColumn1IDs = column1NodeIDs.stream().distinct().collect(Collectors.toList());
    List<String> uniqueColumn2IDs = column2NodeIDs.stream().distinct().collect(Collectors.toList());
    List<String> uniqueNodeIDs = nodeIDs.stream().distinct().collect(Collectors.toList());

    // Turn the node ids into an array of Node objects, each with an id property
    ArrayList<NodeData> nodes = new ArrayList<NodeData>();
    uniqueNodeIDs.forEach((nodeID) -> {
      NodeData node = new NodeDataImpl();
      node.setId(nodeID);
      nodes.add(node);
    });

    // Finally create the bipartite network response and send it off!
    BipartiteNetworkData bipartiteNetworkData = new BipartiteNetworkDataImpl();
    bipartiteNetworkData.setLinks(links);
    bipartiteNetworkData.setNodes(nodes);
    bipartiteNetworkData.setColumn1NodeIDs(uniqueColumn1IDs);
    bipartiteNetworkData.setColumn2NodeIDs(uniqueColumn2IDs);

    BipartiteNetworkConfig bipartiteNetworkConfig = new BipartiteNetworkConfigImpl();
    bipartiteNetworkConfig.setColumn1Metadata(stats.getData1Metadata());
    bipartiteNetworkConfig.setColumn2Metadata(stats.getData2Metadata());

    BipartiteNetwork bipartiteNetwork = new BipartiteNetworkImpl();
    bipartiteNetwork.setData(bipartiteNetworkData);
    bipartiteNetwork.setConfig(bipartiteNetworkConfig);

    BipartiteNetworkPostResponse response = new BipartiteNetworkPostResponseImpl();
    response.setBipartitenetwork(bipartiteNetwork);

    JsonUtil.Jackson.writeValue(out, response);
    out.flush();

  }
}
