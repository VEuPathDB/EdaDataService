package org.veupathdb.service.eda.ds.plugin.standalonemap;

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

import static org.veupathdb.service.eda.common.plugin.util.PluginUtil.singleQuote;
import static org.veupathdb.service.eda.common.plugin.util.RServeClient.streamResult;
import static org.veupathdb.service.eda.common.plugin.util.RServeClient.useRConnectionWithRemoteFiles;
import static org.veupathdb.service.eda.ds.metadata.AppsMetadata.VECTORBASE_PROJECT;

public class FloatingLineplotPlugin extends AbstractEmptyComputePlugin<FloatingLineplotPostRequest, FloatingLineplotSpec> {

  @Override
  public String getDisplayName() {
    return "Line plot";
  }

  @Override
  public String getDescription() {
    return "Visualize aggregate values of one variable across the sequential values of an ordered variable";
  }

  @Override
  public List<String> getProjects() {
    return List.of(VECTORBASE_PROJECT);
  }

  @Override
  protected Class<FloatingLineplotPostRequest> getVisualizationRequestClass() {
    return FloatingLineplotPostRequest.class;
  }

  @Override
  protected Class<FloatingLineplotSpec> getVisualizationSpecClass() {
    return FloatingLineplotSpec.class;
  }
  
  @Override
  public ConstraintSpec getConstraintSpec() {
    return new ConstraintSpec()
      .dependencyOrder(List.of("yAxisVariable"), List.of("xAxisVariable"), List.of("overlayVariable"))
      .pattern()
        .element("yAxisVariable")
          .description("Variable must be a number and be of the same or a child entity as the X-axis variable.")
        .element("xAxisVariable")
          .shapes(APIVariableDataShape.ORDINAL, APIVariableDataShape.CONTINUOUS)
          .description("Variable must be ordinal, a number, or a date and be the same or a child entity as the variable the map markers are painted with.")
        .element("overlayVariable")
          .required(false)
      .done();
  }

  @Override
  protected void validateVisualizationSpec(FloatingLineplotSpec pluginSpec) throws ValidationException {
    validateInputs(new DataElementSet()
      .entity(pluginSpec.getOutputEntityId())
      .var("xAxisVariable", pluginSpec.getXAxisVariable())
      .var("yAxisVariable", pluginSpec.getYAxisVariable())
      .var("overlayVariable", pluginSpec.getOverlayVariable()));
  }

  @Override
  protected List<StreamSpec> getRequestedStreams(FloatingLineplotSpec pluginSpec) {
    return ListBuilder.asList(
      new StreamSpec(DEFAULT_SINGLE_STREAM_NAME, pluginSpec.getOutputEntityId())
        .addVar(pluginSpec.getXAxisVariable())
        .addVar(pluginSpec.getYAxisVariable())
        .addVar(pluginSpec.getOverlayVariable()));
  }

  @Override
  protected void writeResults(OutputStream out, Map<String, InputStream> dataStreams) throws IOException {
    PluginUtil util = getUtil();
    FloatingLineplotSpec spec = getPluginSpec();
    Map<String, VariableSpec> varMap = new HashMap<String, VariableSpec>();
    varMap.put("xAxis", spec.getXAxisVariable());
    varMap.put("yAxis", spec.getYAxisVariable());
    varMap.put("overlay", spec.getOverlayVariable());
    String errorBars = spec.getErrorBars() != null ? spec.getErrorBars().getValue() : "FALSE";
    String valueSpec = spec.getValueSpec().getValue();
    String xVar = util.toColNameOrEmpty(spec.getXAxisVariable());
    String xVarType = util.getVariableType(spec.getXAxisVariable());
    String numeratorValues = spec.getYAxisNumeratorValues() != null ? listToRVector(spec.getYAxisNumeratorValues()) : "NULL";
    String denominatorValues = spec.getYAxisDenominatorValues() != null ? listToRVector(spec.getYAxisDenominatorValues()) : "NULL";
    String overlayValues = listToRVector(spec.getOverlayValues().stream().map(BinRange::getBinLabel).collect(Collectors.toList()));
    
    useRConnectionWithRemoteFiles(Resources.RSERVE_URL, dataStreams, connection -> {
      connection.voidEval(util.getVoidEvalFreadCommand(DEFAULT_SINGLE_STREAM_NAME,
          spec.getXAxisVariable(),
          spec.getYAxisVariable(),
          spec.getOverlayVariable()));
      connection.voidEval(getVoidEvalVariableMetadataList(varMap));
      String viewportRString = getViewportAsRString(spec.getViewport(), xVarType);
      connection.voidEval(viewportRString);
      BinSpec binSpec = spec.getBinSpec();
      validateBinSpec(binSpec, xVarType);
      // consider refactoring this, does the same as something in histo
      String binType = binSpec.getType().getValue() != null ? binSpec.getType().getValue() : "none";
      if (binType.equals("numBins")) {
        if (binSpec.getValue() != null) {
          String numBins = binSpec.getValue().toString();
          if (xVarType.equals("NUMBER") || xVarType.equals("INTEGER")) {
            connection.voidEval("binWidth <- plot.data::numBinsToBinWidth(data$" + xVar + ", " + numBins + ")");
          } else {
            connection.voidEval("binWidth <- ceiling(as.numeric(diff(range(as.Date(data$" + xVar + ")))/" + numBins + "))");
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
      String cmd = "plot.data::lineplot(data=" + DEFAULT_SINGLE_STREAM_NAME + 
                                        ", variables=variables, binWidth=binWidth, " + 
                                        "  value=" + singleQuote(valueSpec) + 
                                        ", errorBars=" + errorBars + 
                                        ", viewport=viewport" + 
                                        ", numeratorValues=" + numeratorValues +
                                        ", denominatorValues=" + denominatorValues + 
                                        ", samplesSizes=FALSE" +
                                        ", completeCases=FALSE" +
                                        ", overlayValues=" + overlayValues + 
                                        ", 'noVariables')";                          
      streamResult(connection, cmd, out);
    }); 
  }
}
