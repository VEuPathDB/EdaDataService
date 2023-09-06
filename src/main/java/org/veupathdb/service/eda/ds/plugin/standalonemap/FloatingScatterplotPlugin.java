package org.veupathdb.service.eda.ds.plugin.standalonemap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gusdb.fgputil.ListBuilder;
import org.gusdb.fgputil.validation.ValidationException;
import org.veupathdb.service.eda.common.client.spec.StreamSpec;
import org.veupathdb.service.eda.common.plugin.constraint.ConstraintSpec;
import org.veupathdb.service.eda.common.plugin.constraint.DataElementSet;
import org.veupathdb.service.eda.common.plugin.util.PluginUtil;
import org.veupathdb.service.eda.common.plugin.util.RFileSetProcessor;
import org.veupathdb.service.eda.ds.Resources;
import org.veupathdb.service.eda.ds.core.AbstractEmptyComputePlugin;
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

import static org.veupathdb.service.eda.common.plugin.util.RServeClient.streamResult;
import static org.veupathdb.service.eda.common.plugin.util.RServeClient.useRConnectionWithProcessedRemoteFiles;
import static org.veupathdb.service.eda.ds.metadata.AppsMetadata.VECTORBASE_PROJECT;

public class FloatingScatterplotPlugin extends AbstractEmptyComputePlugin<FloatingScatterplotPostRequest, FloatingScatterplotSpec> {
  private OverlaySpecification _overlaySpecification = null;

  private static final Logger LOG = LogManager.getLogger(FloatingScatterplotPlugin.class);
  
  @Override
  public String getDisplayName() {
    return "Scatter plot";
  }

  @Override
  public String getDescription() {
    return "Visualize the relationship between two continuous variables";
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
          .types(APIVariableType.NUMBER, APIVariableType.DATE, APIVariableType.INTEGER) 
          .description("Variable must be a number or date and be of the same or a child entity as the X-axis variable.")
        .element("xAxisVariable")
          .types(APIVariableType.NUMBER, APIVariableType.DATE, APIVariableType.INTEGER)
          .description("Variable must be a number or date and be the same or a child entity as the variable the map markers are painted with, if any.")
        .element("overlayVariable")
          .required(false)
      .done();
  }

  @Override
  protected ClassGroup getTypeParameterClasses() {
    return new EmptyComputeClassGroup(FloatingScatterplotPostRequest.class, FloatingScatterplotSpec.class);
  }

  @Override
  protected void validateVisualizationSpec(FloatingScatterplotSpec pluginSpec) throws ValidationException {
    validateInputs(new DataElementSet()
      .entity(pluginSpec.getOutputEntityId())
      .var("xAxisVariable", pluginSpec.getXAxisVariable())
      .var("yAxisVariable", pluginSpec.getYAxisVariable())
      .var("overlayVariable", Optional.ofNullable(pluginSpec.getOverlayConfig())
          .map(OverlayConfig::getOverlayVariable)
          .orElse(null)));
    if (pluginSpec.getOverlayConfig() != null) {
      try {
        _overlaySpecification = new OverlaySpecification(pluginSpec.getOverlayConfig(), getUtil()::getVariableType, getUtil()::getVariableDataShape);
      } catch (IllegalArgumentException e) {
        throw new ValidationException(e.getMessage());
      }
    }
    if (pluginSpec.getMaxAllowedDataPoints() != null && pluginSpec.getMaxAllowedDataPoints() <= 0) {
      throw new ValidationException("maxAllowedDataPoints must be a positive integer");
    }
  }

  @Override
  protected List<StreamSpec> getRequestedStreams(FloatingScatterplotSpec pluginSpec) {
    return ListBuilder.asList(
      new StreamSpec(DEFAULT_SINGLE_STREAM_NAME, pluginSpec.getOutputEntityId())
        .addVar(pluginSpec.getXAxisVariable())
        .addVar(pluginSpec.getYAxisVariable())
        .addVar(Optional.ofNullable(pluginSpec.getOverlayConfig())
            .map(OverlayConfig::getOverlayVariable)
            .orElse(null)));
  }

  @Override
  protected void writeResults(OutputStream out, Map<String, InputStream> dataStreams) throws IOException {
    PluginUtil util = getUtil();
    FloatingScatterplotSpec spec = getPluginSpec();
    VariableSpec overlayVariable = _overlaySpecification != null ? _overlaySpecification.getOverlayVariable() : null;
    Map<String, VariableSpec> varMap = new HashMap<>();
    varMap.put("xAxis", spec.getXAxisVariable());
    varMap.put("yAxis", spec.getYAxisVariable());
    varMap.put("overlay", overlayVariable);
    String valueSpec = spec.getValueSpec().getValue();
    String yVarType = util.getVariableType(spec.getYAxisVariable());
    String overlayValues = _overlaySpecification == null ? "NULL" : _overlaySpecification.getRBinListAsString();
    
    if (yVarType.equals("DATE") && !valueSpec.equals("raw")) {
      LOG.error("Cannot calculate trend lines for y-axis date variables. The `valueSpec` property must be set to `raw`.");
    }
    
    List<String> nonStrataVarColNames = new ArrayList<>();
    nonStrataVarColNames.add(util.toColNameOrEmpty(spec.getXAxisVariable()));
    nonStrataVarColNames.add(util.toColNameOrEmpty(spec.getYAxisVariable()));

    List<DynamicDataSpecImpl> dataSpecsWithStudyDependentVocabs = findVariableSpecsWithStudyDependentVocabs(varMap);
    Map<String, InputStream> studyVocabs = getVocabByRootEntity(dataSpecsWithStudyDependentVocabs);
    dataStreams.putAll(studyVocabs);

    RFileSetProcessor filesProcessor = new RFileSetProcessor(dataStreams)
      .add(DEFAULT_SINGLE_STREAM_NAME, 
        spec.getMaxAllowedDataPoints(), 
        "noVariables", 
        nonStrataVarColNames, 
        (name, conn) ->
        conn.voidEval(util.getVoidEvalFreadCommand(name,
          spec.getXAxisVariable(),
          spec.getYAxisVariable(),
          overlayVariable))
      );

    useRConnectionWithProcessedRemoteFiles(Resources.RSERVE_URL, filesProcessor, connection -> {
      String inputData = getRVariableInputDataWithImputedZeroesAsString(DEFAULT_SINGLE_STREAM_NAME, varMap);
      connection.voidEval(getVoidEvalVariableMetadataList(varMap));
      
      String cmd = 
          "plot.data::scattergl(data=" + inputData + ", " + 
                                  "variables=variables, " + 
                                  "value='" + valueSpec + "', " + 
                                  "sampleSizes=FALSE, " +
                                  "completeCases=FALSE, " + 
                                  "overlayValues=" + overlayValues + ", " + 
                                  "evilMode='noVariables')";
      streamResult(connection, cmd, out);
    }); 
  }
}
