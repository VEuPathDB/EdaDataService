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
import org.veupathdb.service.eda.ds.plugin.AbstractEmptyComputePlugin;
import org.veupathdb.service.eda.generated.model.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.veupathdb.service.eda.common.plugin.util.RServeClient.streamResult;
import static org.veupathdb.service.eda.common.plugin.util.RServeClient.useRConnectionWithRemoteFiles;
import static org.veupathdb.service.eda.ds.metadata.AppsMetadata.VECTORBASE_PROJECT;

public class FloatingHistogramPlugin extends AbstractEmptyComputePlugin<FloatingHistogramPostRequest, FloatingHistogramSpec> {
  
  private static final Logger LOG = LogManager.getLogger(FloatingHistogramPlugin.class);

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
  protected Class<FloatingHistogramPostRequest> getVisualizationRequestClass() {
    return FloatingHistogramPostRequest.class;
  }

  @Override
  protected Class<FloatingHistogramSpec> getVisualizationSpecClass() {
    return FloatingHistogramSpec.class;
  }
  
  @Override
  public ConstraintSpec getConstraintSpec() {
    return new ConstraintSpec()
      .dependencyOrder(List.of("xAxisVariable"), List.of("overlayVariable"))
      .pattern()
        .element("xAxisVariable")
          .types(APIVariableType.NUMBER, APIVariableType.DATE, APIVariableType.INTEGER)
          .description("Variable must be a number or date and be the same or a child entity as the variable the map markers are painted with.")
        .element("overlayVariable")
          .required(false)
      .done();
  }
  
  @Override
  protected void validateVisualizationSpec(FloatingHistogramSpec pluginSpec) throws ValidationException {
    validateInputs(new DataElementSet()
      .entity(pluginSpec.getOutputEntityId())
      .var("xAxisVariable", pluginSpec.getXAxisVariable())
      .var("overlayVariable", pluginSpec.getOverlayVariable()));
    if (pluginSpec.getBarMode() == null) {
      throw new ValidationException("Property 'barMode' is required.");
    }
  }

  @Override
  protected List<StreamSpec> getRequestedStreams(FloatingHistogramSpec pluginSpec) {
    return ListBuilder.asList(
      new StreamSpec(DEFAULT_SINGLE_STREAM_NAME, pluginSpec.getOutputEntityId())
        .addVar(pluginSpec.getXAxisVariable())
        .addVar(pluginSpec.getOverlayVariable()));
  }
  
  @Override
  protected void writeResults(OutputStream out, Map<String, InputStream> dataStreams) throws IOException {
    PluginUtil util = getUtil();
    FloatingHistogramSpec spec = getPluginSpec();
    Map<String, VariableSpec> varMap = new HashMap<String, VariableSpec>();
    varMap.put("xAxis", spec.getXAxisVariable());
    varMap.put("overlay", spec.getOverlayVariable());
    String barMode = spec.getBarMode().getValue();
    String xVar = util.toColNameOrEmpty(spec.getXAxisVariable());
    String xVarType = util.getVariableType(spec.getXAxisVariable());
    String overlayValues = listToRVector(spec.getOverlayValues().stream().map(BinRange::getBinLabel).collect(Collectors.toList()));

    useRConnectionWithRemoteFiles(Resources.RSERVE_URL, dataStreams, connection -> {
      connection.voidEval(util.getVoidEvalFreadCommand(DEFAULT_SINGLE_STREAM_NAME,
          spec.getXAxisVariable(),
          spec.getOverlayVariable()));
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
          connection.voidEval("xVP <- adjustToViewport(data$" + xVar + ", viewport)");
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
        String binWidth = "NULL";
        if (xVarType.equals("NUMBER") || xVarType.equals("INTEGER")) {
          binWidth = binSpec.getValue() == null ? "NULL" : "as.numeric('" + binSpec.getValue() + "')";
        } else {
          binWidth = binSpec.getValue() == null ? "NULL" : "'" + binSpec.getValue().toString() + " " + binSpec.getUnits().toString().toLowerCase() + "'";
        }
        connection.voidEval("binWidth <- " + binWidth);
      }

      String cmd =
          "plot.data::histogram(" + DEFAULT_SINGLE_STREAM_NAME + ", variables, binWidth, '" +
               spec.getValueSpec().getValue() + "', '" +
               binReportValue + "', '" +
               barMode + "', viewport=viewport, sampleSizes=FALSE, completeCases=FALSE, " +
               "overlayValues=" + overlayValues + ", 'noVariables')";
      streamResult(connection, cmd, out);
    });
  }
}
