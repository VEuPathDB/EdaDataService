package org.veupathdb.service.eda.ds.plugin.pass;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.gusdb.fgputil.ListBuilder;
import org.gusdb.fgputil.validation.ValidationException;
import org.veupathdb.service.eda.common.client.spec.StreamSpec;
import org.veupathdb.service.eda.ds.constraints.ConstraintSpec;
import org.veupathdb.service.eda.ds.constraints.DataElementSet;
import org.veupathdb.service.eda.ds.plugin.AbstractPlugin;
import org.veupathdb.service.eda.generated.model.APIVariableType;
import org.veupathdb.service.eda.generated.model.DensityplotPostRequest;
import org.veupathdb.service.eda.generated.model.DensityplotSpec;
import org.veupathdb.service.eda.generated.model.VariableSpec;

import static org.veupathdb.service.eda.ds.metadata.AppsMetadata.CLINEPI_PROJECT;
import static org.veupathdb.service.eda.ds.metadata.AppsMetadata.MICROBIOME_PROJECT;
import static org.veupathdb.service.eda.ds.util.RServeClient.streamResult;
import static org.veupathdb.service.eda.ds.util.RServeClient.useRConnectionWithRemoteFiles;

public class DensityplotPlugin extends AbstractPlugin<DensityplotPostRequest, DensityplotSpec> {

  @Override
  public String getDisplayName() {
    return "Density plot";
  }

  @Override
  public String getDescription() {
    return "Visualize the smoothed distribution (using a kernel density estimate) of a continuous variable";
  }
  
  @Override
  public List<String> getProjects() {
    return List.of(CLINEPI_PROJECT, MICROBIOME_PROJECT);
  }
  
  @Override
  protected Class<DensityplotSpec> getVisualizationSpecClass() {
    return DensityplotSpec.class;
  }

  @Override
  public ConstraintSpec getConstraintSpec() {
    return new ConstraintSpec()
      .dependencyOrder("xAxisVariable", "overlayVariable", "facetVariable")
      .pattern()
        .element("xAxisVariable")
          .types(APIVariableType.NUMBER, APIVariableType.INTEGER)
          .description("Variable must be a number.")
        .element("overlayVariable")
          .maxValues(8)
          .description("Variable must have 8 or fewer unique values and be of the same or a parent entity of the X-axis variable.")
        .element("facetVariable")
          .required(false)
          .maxVars(2)
          .maxValues(10)
          .description("Variable(s) must have 10 or fewer unique values and be of the same or a parent entity of the Overlay variable.")
      .done();
  }
  
  @Override
  protected void validateVisualizationSpec(DensityplotSpec pluginSpec) throws ValidationException {
    validateInputs(new DataElementSet()
      .entity(pluginSpec.getOutputEntityId())
      .var("xAxisVariable", pluginSpec.getXAxisVariable())
      .var("overlayVariable", pluginSpec.getOverlayVariable())
      .var("facetVariable", pluginSpec.getFacetVariable()));
  }

  @Override
  protected List<StreamSpec> getRequestedStreams(DensityplotSpec pluginSpec) {
    return ListBuilder.asList(
      new StreamSpec(DEFAULT_SINGLE_STREAM_NAME, pluginSpec.getOutputEntityId())
        .addVar(pluginSpec.getXAxisVariable())
        .addVar(pluginSpec.getOverlayVariable())
        .addVars(pluginSpec.getFacetVariable()));
  }

  @Override
  protected void writeResults(OutputStream out, Map<String, InputStream> dataStreams) throws IOException {
    DensityplotSpec spec = getPluginSpec();
    Map<String, VariableSpec> varMap = new HashMap<String, VariableSpec>();
    varMap.put("xAxisVariable", spec.getXAxisVariable());
    varMap.put("overlayVariable", spec.getOverlayVariable());
    varMap.put("facetVariable1", getVariableSpecFromList(spec.getFacetVariable(), 0));
    varMap.put("facetVariable2", getVariableSpecFromList(spec.getFacetVariable(), 1));
    String showMissingness = spec.getShowMissingness() != null ? spec.getShowMissingness().getValue() : "FALSE";
    
    useRConnectionWithRemoteFiles(dataStreams, connection -> {
      connection.voidEval(getVoidEvalFreadCommand(DEFAULT_SINGLE_STREAM_NAME, 
          spec.getXAxisVariable(),
          spec.getOverlayVariable(),
          getVariableSpecFromList(spec.getFacetVariable(), 0),
          getVariableSpecFromList(spec.getFacetVariable(), 1)));
      connection.voidEval(getVoidEvalVarMetadataMap(DEFAULT_SINGLE_STREAM_NAME, varMap));
      String cmd = "plot.data::scattergl(" + DEFAULT_SINGLE_STREAM_NAME + ", map, 'density'," + showMissingness + ")";
      streamResult(connection, cmd, out);
    }); 
  }
}
