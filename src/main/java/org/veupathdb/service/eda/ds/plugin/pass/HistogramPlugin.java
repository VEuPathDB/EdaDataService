package org.veupathdb.service.eda.ds.plugin.pass;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gusdb.fgputil.ListBuilder;
import org.gusdb.fgputil.validation.ValidationException;
import org.veupathdb.service.eda.common.client.spec.StreamSpec;
import org.veupathdb.service.eda.ds.constraints.ConstraintSpec;
import org.veupathdb.service.eda.ds.constraints.DataElementSet;
import org.veupathdb.service.eda.ds.plugin.AbstractPlugin;
import org.veupathdb.service.eda.generated.model.APIVariableType;
import org.veupathdb.service.eda.generated.model.BinSpec;
import org.veupathdb.service.eda.generated.model.HistogramPostRequest;
import org.veupathdb.service.eda.generated.model.HistogramSpec;
import org.veupathdb.service.eda.generated.model.VariableSpec;

import static org.veupathdb.service.eda.ds.metadata.AppsMetadata.CLINEPI_PROJECT;
import static org.veupathdb.service.eda.ds.metadata.AppsMetadata.MICROBIOME_PROJECT;
import static org.veupathdb.service.eda.ds.util.RServeClient.streamResult;
import static org.veupathdb.service.eda.ds.util.RServeClient.useRConnectionWithRemoteFiles;

public class HistogramPlugin extends AbstractPlugin<HistogramPostRequest, HistogramSpec> {
  
  private static final Logger LOG = LogManager.getLogger(HistogramPlugin.class);

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
    return List.of(CLINEPI_PROJECT, MICROBIOME_PROJECT);
  }
  
  @Override
  protected Class<HistogramSpec> getVisualizationSpecClass() {
    return HistogramSpec.class;
  }
  
  @Override
  public ConstraintSpec getConstraintSpec() {
    return new ConstraintSpec()
      .dependencyOrder("xAxisVariable", "overlayVariable", "facetVariable")
      .pattern()
        .element("xAxisVariable")
          .types(APIVariableType.NUMBER, APIVariableType.DATE, APIVariableType.INTEGER)
          .description("Variable must be a number or date.")
        .element("overlayVariable")
          .maxValues(8)
          .description("Variable must have 8 or fewer unique values and be of the same or a parent entity as the X-axis variable.")
        .element("facetVariable")
          .required(false)
          .maxVars(2)
          .maxValues(7)
          .description("Variable(s) must have 7 or fewer unique values and be of the same or a parent entity as the Overlay variable.")
      .done();
  }
  
  @Override
  protected void validateVisualizationSpec(HistogramSpec pluginSpec) throws ValidationException {
    validateInputs(new DataElementSet()
      .entity(pluginSpec.getOutputEntityId())
      .var("xAxisVariable", pluginSpec.getXAxisVariable())
      .var("overlayVariable", pluginSpec.getOverlayVariable())
      .var("facetVariable", pluginSpec.getFacetVariable()));
    if (pluginSpec.getBarMode() == null) {
      throw new ValidationException("Property 'barMode' is required.");
    }
  }

  @Override
  protected List<StreamSpec> getRequestedStreams(HistogramSpec pluginSpec) {
    return ListBuilder.asList(
      new StreamSpec(DEFAULT_SINGLE_STREAM_NAME, pluginSpec.getOutputEntityId())
        .addVar(pluginSpec.getXAxisVariable())
        .addVar(pluginSpec.getOverlayVariable())
        .addVars(pluginSpec.getFacetVariable()));
  }
  
  @Override
  protected void writeResults(OutputStream out, Map<String, InputStream> dataStreams) throws IOException {
    HistogramSpec spec = getPluginSpec(); 
    Map<String, VariableSpec> varMap = new HashMap<String, VariableSpec>();
    varMap.put("xAxisVariable", spec.getXAxisVariable());
    varMap.put("overlayVariable", spec.getOverlayVariable());
    varMap.put("facetVariable1", getVariableSpecFromList(spec.getFacetVariable(), 0));
    varMap.put("facetVariable2", getVariableSpecFromList(spec.getFacetVariable(), 1));
    String showMissingness = spec.getShowMissingness() != null ? spec.getShowMissingness().getValue() : "FALSE";
    String barMode = spec.getBarMode().getValue();
    String xVar = toColNameOrEmpty(spec.getXAxisVariable());
    String xVarType = getVariableType(spec.getXAxisVariable());

    useRConnectionWithRemoteFiles(dataStreams, connection -> {
      connection.voidEval(getVoidEvalFreadCommand(DEFAULT_SINGLE_STREAM_NAME, 
          spec.getXAxisVariable(),
          spec.getOverlayVariable(),
          getVariableSpecFromList(spec.getFacetVariable(), 0),
          getVariableSpecFromList(spec.getFacetVariable(), 1)));
      connection.voidEval(getVoidEvalVarMetadataMap(DEFAULT_SINGLE_STREAM_NAME, varMap));
     
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
          "plot.data::histogram(" + DEFAULT_SINGLE_STREAM_NAME + ", map, binWidth, '" +
               spec.getValueSpec().getValue() + "', '" +
               binReportValue + "', '" +
               barMode + "', viewport, " +
               showMissingness + ")";
      streamResult(connection, cmd, out);
    });
  }
}
