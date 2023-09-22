package org.veupathdb.service.eda.ds.plugin.standalonemap;

import org.gusdb.fgputil.ListBuilder;
import org.gusdb.fgputil.validation.ValidationException;
import org.veupathdb.service.eda.common.client.spec.StreamSpec;
import org.veupathdb.service.eda.common.plugin.constraint.ConstraintSpec;
import org.veupathdb.service.eda.common.plugin.constraint.DataElementSet;
import org.veupathdb.service.eda.common.plugin.util.PluginUtil;
import org.veupathdb.service.eda.common.plugin.util.RFileSetProcessor;
import org.veupathdb.service.eda.Resources;
import org.veupathdb.service.eda.ds.core.AbstractEmptyComputePlugin;
import org.veupathdb.service.eda.ds.utils.ValidationUtils;
import org.veupathdb.service.eda.generated.model.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.veupathdb.service.eda.common.plugin.util.RServeClient.streamResult;
import static org.veupathdb.service.eda.common.plugin.util.RServeClient.useRConnectionWithProcessedRemoteFiles;
import static org.veupathdb.service.eda.ds.metadata.AppsMetadata.VECTORBASE_PROJECT;

public class CollectionFloatingBoxplotPlugin extends AbstractEmptyComputePlugin<CollectionFloatingBoxplotPostRequest, CollectionFloatingBoxplotSpec> {

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
          .description("Variable must have 10 or fewer unique values and be the same or a child entity as the variable the map markers are painted with.")
        .element("overlayVariable")
      .done();
  }

  @Override
  protected ClassGroup getTypeParameterClasses() {
    return new EmptyComputeClassGroup(CollectionFloatingBoxplotPostRequest.class, CollectionFloatingBoxplotSpec.class);
  }

  @Override
  protected void validateVisualizationSpec(CollectionFloatingBoxplotSpec pluginSpec) throws ValidationException {
    validateInputs(new DataElementSet()
      .entity(pluginSpec.getOutputEntityId())
      .var("xAxisVariable", pluginSpec.getXAxisVariable()));
    ValidationUtils.validateCollectionMembers(getUtil(),
        pluginSpec.getOverlayConfig().getCollection(),
        pluginSpec.getOverlayConfig().getSelectedMembers());
  }

  @Override
  protected List<StreamSpec> getRequestedStreams(CollectionFloatingBoxplotSpec pluginSpec) {
    return ListBuilder.asList(
      new StreamSpec(DEFAULT_SINGLE_STREAM_NAME, pluginSpec.getOutputEntityId())
        .addVars(pluginSpec.getOverlayConfig().getSelectedMembers())
        .addVar(pluginSpec.getXAxisVariable())
      );
  }

  @Override
  protected void writeResults(OutputStream out, Map<String, InputStream> dataStreams) throws IOException {
    PluginUtil util = getUtil();
    CollectionFloatingBoxplotSpec spec = getPluginSpec();
    List<VariableSpec> inputVarSpecs = new ArrayList<>(spec.getOverlayConfig().getSelectedMembers());
    inputVarSpecs.add(spec.getXAxisVariable());
    CollectionSpec overlayVariable = spec.getOverlayConfig().getCollection();
    Map<String, DynamicDataSpecImpl> varMap = new HashMap<>();
    varMap.put("xAxis", new DynamicDataSpecImpl(spec.getXAxisVariable()));
    varMap.put("overlay", new DynamicDataSpecImpl(overlayVariable));
    
    List<String> nonStrataVarColNames = new ArrayList<>();
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
      connection.voidEval(getVoidEvalDynamicDataMetadataList(varMap));
      String cmd =
          "plot.data::box(data=" + DEFAULT_SINGLE_STREAM_NAME + ", " +
              "variables=variables, " +
              "points='outliers', " +
              "mean=TRUE, " +
              "computeStats=FALSE, " +
              "sampleSizes=FALSE, " +
              "completeCases=FALSE, " +
              "overlayValues=NULL, " +
              "evilMode='noVariables')";
      streamResult(connection, cmd, out);
    });
  }
}
