package org.veupathdb.service.eda.ds.plugin.standalonemap;

import org.gusdb.fgputil.ListBuilder;
import org.gusdb.fgputil.validation.ValidationException;
import org.veupathdb.service.eda.common.client.spec.StreamSpec;
import org.veupathdb.service.eda.common.plugin.constraint.ConstraintSpec;
import org.veupathdb.service.eda.common.plugin.constraint.DataElementSet;
import org.veupathdb.service.eda.common.plugin.util.PluginUtil;
import org.veupathdb.service.eda.common.plugin.util.RFileSetProcessor;
import org.veupathdb.service.eda.ds.Resources;
import org.veupathdb.service.eda.ds.plugin.AbstractEmptyComputePlugin;
import org.veupathdb.service.eda.ds.plugin.AbstractPlugin;
import org.veupathdb.service.eda.ds.plugin.standalonemap.markers.OverlaySpecification;
import org.veupathdb.service.eda.generated.model.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.veupathdb.service.eda.common.plugin.util.RServeClient.streamResult;
import static org.veupathdb.service.eda.common.plugin.util.RServeClient.useRConnectionWithProcessedRemoteFiles;
import static org.veupathdb.service.eda.ds.metadata.AppsMetadata.VECTORBASE_PROJECT;

public class CollectionFloatingBoxplotPlugin extends AbstractEmptyComputePlugin<FloatingBoxplotPostRequest, FloatingBoxplotSpec> {
  private OverlaySpecification _overlaySpecification = null;

  @Override
  public String getDisplayName() {
    return "Box plot";
  }

  @Override
  public String getDescription() {
    return "Visualize summary values for a continuous Variable Group";
  }

  @Override
  public List<String> getProjects() {
    return List.of(VECTORBASE_PROJECT);
  }

  @Override
  public ConstraintSpec getConstraintSpec() {
    return new ConstraintSpec()
      .dependencyOrder(List.of("yAxisVariable"), List.of("xAxisVariable", "overlayVariable"))
      .pattern()
        .element("yAxisVariable")
          .types(APIVariableType.NUMBER, APIVariableType.INTEGER)
          .description("Variable Group must be a number.")
        .element("xAxisVariable")
          .maxValues(10)
          .description("Variable must have 10 or fewer unique values and be the same or a child entity as the variable the map markers are painted with, if any.")
        .element("overlayVariable")
      .done();
  }

  @Override
  protected AbstractPlugin<FloatingBoxplotPostRequest, FloatingBoxplotSpec, Void>.ClassGroup getTypeParameterClasses() {
    return new ClassGroup(FloatingBoxplotPostRequest.class, FloatingBoxplotSpec.class, Void.class);
  }

  @Override
  protected void validateVisualizationSpec(FloatingBoxplotSpec pluginSpec) throws ValidationException {
    validateInputs(new DataElementSet()
      .entity(pluginSpec.getOutputEntityId())
      .var("xAxisVariable", pluginSpec.getXAxisVariable()));
    // TODO general collection validation and make sure its continuous
  }

  @Override
  protected List<StreamSpec> getRequestedStreams(FloatingBoxplotSpec pluginSpec) {
    return ListBuilder.asList(
      new StreamSpec(DEFAULT_SINGLE_STREAM_NAME, pluginSpec.getOutputEntityId())
        .addVars(getUtil().getChildrenVariables(pluginSpec.getCollectionOverlayConfig().getCollection())
        .addVar(pluginSpec.getXAxisVariable())
      ));
  }

  @Override
  protected void writeResults(OutputStream out, Map<String, InputStream> dataStreams) throws IOException {
    PluginUtil util = getUtil();
    FloatingBoxplotSpec spec = getPluginSpec();
    List<VariableSpec> inputVarSpecs = new ArrayList<>(spec.getCollectionOverlayConfig().getSelectedMembers());
    inputVarSpecs.add(spec.getXAxisVariable());
    CollectionSpec overlayVariable = spec.getCollectionOverlayConfigWithValues().getCollection();
    Map<String, VariableSpec> varMap = new HashMap<String, VariableSpec>();
    varMap.put("xAxis", spec.getXAxisVariable());
    varMap.put("overlay", overlayVariable);
    
    List<String> nonStrataVarColNames = new ArrayList<String>();
    nonStrataVarColNames.add(util.toColNameOrEmpty(spec.getXAxisVariable()));
    // ideally wed find another way to account for the yaxis given its a collection but that seems hard and idk if were even using this feature
    //nonStrataVarColNames.add(util.toColNameOrEmpty(spec.getYAxisVariable()));

    RFileSetProcessor filesProcessor = new RFileSetProcessor(dataStreams)
      .add(DEFAULT_SINGLE_STREAM_NAME, 
        spec.getMaxAllowedDataPoints(), 
        "noVariables", 
        nonStrataVarColNames, 
        (name, conn) ->
        conn.voidEval(util.getVoidEvalFreadCommand(name, inputVarSpecs))
      );

    useRConnectionWithProcessedRemoteFiles(Resources.RSERVE_URL, filesProcessor, connection -> {
      String overlayValues = getRBinListAsString(spec.getCollectionOverlayConfig().getSelectedValues());
      connection.voidEval(getVoidEvalVariableMetadataList(varMap));
      String cmd =
          "plot.data::box(data=" + DEFAULT_SINGLE_STREAM_NAME + ", " +
              "variables=variables, " +
              "points='outliers', " +
              "mean=TRUE, " +
              "computeStats=FALSE, " +
              "sampleSizes=FALSE, " +
              "completeCases=FALSE, " +
              "overlayValues=" + overlayValues + ", " +
              "evilMode='noVariables')";
      streamResult(connection, cmd, out);
    });
  }
}
