package org.veupathdb.service.eda.ds.plugin.standalonemap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gusdb.fgputil.ListBuilder;
import org.gusdb.fgputil.validation.ValidationException;
import org.veupathdb.service.eda.common.client.spec.StreamSpec;
import org.veupathdb.service.eda.common.plugin.constraint.ConstraintSpec;
import org.veupathdb.service.eda.common.plugin.constraint.DataElementSet;
import org.veupathdb.service.eda.common.plugin.util.PluginUtil;
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
import static org.veupathdb.service.eda.common.plugin.util.RServeClient.useRConnectionWithRemoteFiles;
import static org.veupathdb.service.eda.ds.metadata.AppsMetadata.VECTORBASE_PROJECT;

public class FloatingHistogramPlugin extends AbstractEmptyComputePlugin<FloatingHistogramPostRequest, FloatingHistogramSpec> {

  private OverlaySpecification _overlaySpecification = null;

  @Override
  public String getDisplayName() {
    return "Histogram";
  }

  @Override
  public String getDescription() {
    return "Visualize the distribution of a continuous variable";
  }

  @Override
  public List<String> getProjects() {
    return List.of(VECTORBASE_PROJECT);
  }

  @Override
  public ConstraintSpec getConstraintSpec() {
    return new ConstraintSpec()
      .dependencyOrder(List.of("xAxisVariable"), List.of("overlayVariable"))
      .pattern()
        .element("xAxisVariable")
          .types(APIVariableType.NUMBER, APIVariableType.DATE, APIVariableType.INTEGER)
          .description("Variable must be a number or date and be the same or a child entity as the variable the map markers are painted with, if any.")
        .element("overlayVariable")
          .required(false)
      .done();
  }

  @Override
  protected ClassGroup getTypeParameterClasses() {
    return new EmptyComputeClassGroup(FloatingHistogramPostRequest.class, FloatingHistogramSpec.class);
  }

  @Override
  protected void validateVisualizationSpec(FloatingHistogramSpec pluginSpec) throws ValidationException {
    validateInputs(new DataElementSet()
      .entity(pluginSpec.getOutputEntityId())
      .var("xAxisVariable", pluginSpec.getXAxisVariable())
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
    if (pluginSpec.getBarMode() == null) {
      throw new ValidationException("Property 'barMode' is required.");
    }
  }

  @Override
  protected List<StreamSpec> getRequestedStreams(FloatingHistogramSpec pluginSpec) {
    String outputEntityId = pluginSpec.getOutputEntityId();
    List<VariableSpec> plotVariableSpecs = new ArrayList<VariableSpec>();
    plotVariableSpecs.add(pluginSpec.getXAxisVariable());
    plotVariableSpecs.add(Optional.ofNullable(pluginSpec.getOverlayConfig()).map(OverlayConfig::getOverlayVariable).orElse(null));

    return ListBuilder.asList(
      new StreamSpec(DEFAULT_SINGLE_STREAM_NAME, outputEntityId)
        .addVars(plotVariableSpecs)
        // TODO can we make this automagical?
        .addVars(getVariableSpecsWithStudyDependentVocabs(pluginSpec.getOutputEntityId(), plotVariableSpecs)));
  }
  
  @Override
  protected void writeResults(OutputStream out, Map<String, InputStream> dataStreams) throws IOException {
    PluginUtil util = getUtil();
    FloatingHistogramSpec spec = getPluginSpec();
    VariableSpec overlayVariable = _overlaySpecification != null ? _overlaySpecification.getOverlayVariable() : null;
    Map<String, VariableSpec> varMap = new HashMap<>();
    varMap.put("xAxis", spec.getXAxisVariable());
    varMap.put("overlay", overlayVariable);
    String barMode = spec.getBarMode().getValue();
    String xVar = util.toColNameOrEmpty(spec.getXAxisVariable());
    String xVarType = util.getVariableType(spec.getXAxisVariable());
    String overlayValues = _overlaySpecification == null ? "NULL" : _overlaySpecification.getRBinListAsString();

    List<DynamicDataSpec> dataSpecsWithStudyDependentVocabs = findVariableSpecsWithStudyDependentVocabs(varMap);
    Map<String, InputStream> studyVocabs = getVocabByRootEntity(dataSpecsWithStudyDependentVocabs);
    dataStreams.putAll(studyVocabs);

    useRConnectionWithRemoteFiles(Resources.RSERVE_URL, dataStreams, connection -> {
      connection.voidEval(DEFAULT_SINGLE_STREAM_NAME + " <- data.table::fread('" + DEFAULT_SINGLE_STREAM_NAME + "', na.strings=c(''))");
      String inputData = getRVariableInputDataWithImputedZeroesAsString(DEFAULT_SINGLE_STREAM_NAME, varMap, "variables"); 
      connection.voidEval(getVoidEvalVariableMetadataList(varMap));
     
      String viewportRString = getViewportAsRString(spec.getViewport(), xVarType);
      connection.voidEval(viewportRString);
     
      BinSpec binSpec = spec.getBinSpec();
      validateBinSpec(binSpec, xVarType);
      String binReportValue = binSpec.getType().getValue() != null ? binSpec.getType().getValue() : "binWidth";
      
      //consider reorganizing conditions, move check for null value up a level ?
      if (binReportValue.equals("numBins")) {
        if (binSpec.getValue() != null) {
          String numBins = binSpec.getValue().toString();
          connection.voidEval("xVP <- adjustToViewport(" + inputData + "$" + xVar + ", viewport)");
          if (xVarType.equals("NUMBER") || xVarType.equals("INTEGER")) {
            connection.voidEval("xRange <- diff(range(xVP))");
            connection.voidEval("binWidth <- xRange/" + numBins);
          } else {
            connection.voidEval("binWidth <- ceiling(as.numeric(diff(range(as.Date(xVP)))/" + numBins + "))");
            connection.voidEval("binWidth <- paste(binWidth, 'day')");
          }
        } else {
          connection.voidEval("binWidth <- NULL");
        }
      } else {
        String binWidth =
            binSpec.getValue() == null ? "NULL" :
                (xVarType.equals("NUMBER") || xVarType.equals("INTEGER"))
                ? "as.numeric('" + binSpec.getValue() + "')"
                : "'" + binSpec.getValue().toString() + " " + binSpec.getUnits().toString().toLowerCase() + "'";
        connection.voidEval("binWidth <- " + binWidth);
      }

      String cmd =
          "plot.data::histogram(data=" + inputData + ", " +
                                  "variables=variables, " +
                                  "binWidth=binWidth, " +
                                  "value='" + spec.getValueSpec().getValue() + "', " +
                                  "binReportValue='" + binReportValue + "', " +
                                  "barmode='" + barMode + "', " +
                                  "viewport=viewport, " + 
                                  "sampleSizes=FALSE, " +
                                  "completeCases=FALSE, " +
                                  "overlayValues=" + overlayValues + ", " +
                                  "evilMode='noVariables')";

      streamResult(connection, cmd, out);
    });
  }
}
