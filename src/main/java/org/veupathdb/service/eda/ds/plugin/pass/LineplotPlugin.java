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
import org.veupathdb.service.eda.generated.model.APIVariableDataShape;
import org.veupathdb.service.eda.generated.model.APIVariableType;
import org.veupathdb.service.eda.generated.model.BinSpec;
import org.veupathdb.service.eda.generated.model.LineplotPostRequest;
import org.veupathdb.service.eda.generated.model.LineplotSpec;
import org.veupathdb.service.eda.generated.model.VariableSpec;

import static org.veupathdb.service.eda.ds.metadata.AppsMetadata.MICROBIOME_PROJECT;
import static org.veupathdb.service.eda.ds.util.RServeClient.streamResult;
import static org.veupathdb.service.eda.ds.util.RServeClient.useRConnectionWithRemoteFiles;

public class LineplotPlugin extends AbstractPlugin<LineplotPostRequest, LineplotSpec> {

  @Override
  public String getDisplayName() {
    return "Line Plot";
  }

  @Override
  public String getDescription() {
    return "Visualize aggregate values of one variable across the sequential values of an ordered variable";
  }

  @Override
  public List<String> getProjects() {
    return List.of(MICROBIOME_PROJECT);
  }
  
  @Override
  protected Class<LineplotSpec> getVisualizationSpecClass() {
    return LineplotSpec.class;
  }
  
  @Override
  public ConstraintSpec getConstraintSpec() {
    return new ConstraintSpec()
      .dependencyOrder("yAxisVariable", "xAxisVariable", "overlayVariable", "facetVariable")
      .pattern()
        .element("yAxisVariable")
        .element("xAxisVariable")
          .shapes(APIVariableDataShape.ORDINAL, APIVariableDataShape.CONTINUOUS)
          .description("Variable must be a number or date and be of the same or a parent entity as the Y-axis variable.")
        .element("overlayVariable")
          .maxValues(8)
          .description("Variable must have 8 or fewer unique values and be of the same or a parent entity as the X-axis variable.")
        .element("facetVariable")
          .required(false)
          .maxVars(2)
          .maxValues(10)
          .description("Variable(s) must have 10 or fewer unique values and be of the same or a parent entity as the Overlay variable.")
      .done();
  }

  @Override
  protected void validateVisualizationSpec(LineplotSpec pluginSpec) throws ValidationException {
    validateInputs(new DataElementSet()
      .entity(pluginSpec.getOutputEntityId())
      .var("xAxisVariable", pluginSpec.getXAxisVariable())
      .var("yAxisVariable", pluginSpec.getYAxisVariable())
      .var("overlayVariable", pluginSpec.getOverlayVariable())
      .var("facetVariable", pluginSpec.getFacetVariable()));
  }

  @Override
  protected List<StreamSpec> getRequestedStreams(LineplotSpec pluginSpec) {
    return ListBuilder.asList(
      new StreamSpec(DEFAULT_SINGLE_STREAM_NAME, pluginSpec.getOutputEntityId())
        .addVar(pluginSpec.getXAxisVariable())
        .addVar(pluginSpec.getYAxisVariable())
        .addVar(pluginSpec.getOverlayVariable())
        .addVars(pluginSpec.getFacetVariable()));
  }

  @Override
  protected void writeResults(OutputStream out, Map<String, InputStream> dataStreams) throws IOException {
    LineplotSpec spec = getPluginSpec();
    Map<String, VariableSpec> varMap = new HashMap<String, VariableSpec>();
    varMap.put("xAxisVariable", spec.getXAxisVariable());
    varMap.put("yAxisVariable", spec.getYAxisVariable());
    varMap.put("overlayVariable", spec.getOverlayVariable());
    varMap.put("facetVariable1", getVariableSpecFromList(spec.getFacetVariable(), 0));
    varMap.put("facetVariable2", getVariableSpecFromList(spec.getFacetVariable(), 1));
    String showMissingness = spec.getShowMissingness() != null ? spec.getShowMissingness().getValue() : "noVariables";
    String deprecatedShowMissingness = showMissingness.equals("FALSE") ? "noVariables" : showMissingness.equals("TRUE") ? "strataVariables" : showMissingness;
    String errorBars = spec.getErrorBars() != null ? spec.getErrorBars().getValue() : "FALSE";
    String valueSpec = spec.getValueSpec().getValue();
    String xVar = toColNameOrEmpty(spec.getXAxisVariable());
    String xVarType = getVariableType(spec.getXAxisVariable());
    String numeratorValues = spec.getYAxisNumeratorValues() != null ? listToRVector(spec.getYAxisNumeratorValues()) : "NULL";
    String denominatorValues = spec.getYAxisDenominatorValues() != null ? listToRVector(spec.getYAxisDenominatorValues()) : "NULL";
    
    useRConnectionWithRemoteFiles(dataStreams, connection -> {
      connection.voidEval(getVoidEvalFreadCommand(DEFAULT_SINGLE_STREAM_NAME, 
          spec.getXAxisVariable(),
          spec.getYAxisVariable(),
          spec.getOverlayVariable(),
          getVariableSpecFromList(spec.getFacetVariable(), 0),
          getVariableSpecFromList(spec.getFacetVariable(), 1)));          
      connection.voidEval(getVoidEvalVarMetadataMap(DEFAULT_SINGLE_STREAM_NAME, varMap));
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
      String cmd = "plot.data::lineplot(" + DEFAULT_SINGLE_STREAM_NAME + 
                                        ", map, binWidth, " + 
                                        singleQuote(valueSpec) + 
                                        ", " + errorBars + 
                                        ", viewport" + 
                                        ", " + numeratorValues +
                                        ", " + denominatorValues + 
                                        ", '" + deprecatedShowMissingness + "')";                           
      streamResult(connection, cmd, out);
    }); 
  }
}
