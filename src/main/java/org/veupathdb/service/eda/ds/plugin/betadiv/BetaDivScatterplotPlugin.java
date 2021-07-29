package org.veupathdb.service.eda.ds.plugin.betadiv;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gusdb.fgputil.IoUtil;
import org.gusdb.fgputil.ListBuilder;
import org.gusdb.fgputil.validation.ValidationException;
import org.json.JSONObject;
import org.rosuda.REngine.Rserve.RFileInputStream;
import org.veupathdb.service.eda.common.client.spec.StreamSpec;
import org.veupathdb.service.eda.ds.constraints.ConstraintSpec;
import org.veupathdb.service.eda.ds.constraints.DataElementSet;
import org.veupathdb.service.eda.ds.plugin.AbstractPlugin;
import org.veupathdb.service.eda.generated.model.APIVariableDataShape;
import org.veupathdb.service.eda.generated.model.BetaDivScatterplotPostRequest;
import org.veupathdb.service.eda.generated.model.BetaDivScatterplotSpec;
import org.veupathdb.service.eda.generated.model.ScatterplotSpec;

import static org.veupathdb.service.eda.ds.util.RServeClient.useRConnectionWithRemoteFiles;

public class BetaDivScatterplotPlugin extends AbstractPlugin<BetaDivScatterplotPostRequest, BetaDivScatterplotSpec> {

  private static final Logger LOG = LogManager.getLogger(org.veupathdb.service.eda.ds.plugin.betadiv.BetaDivScatterplotPlugin.class);

  @Override
  public String getDisplayName() {
    return "Scatter plot";
  }

  @Override
  public String getDescription() {
    return "VVisualize the between-sample diversity of a set of samples in two dimensions.";
  }

  @Override
  public List<String> getProjects() {
    return Arrays.asList("MicrobiomeDB");
  }

  @Override
  public Integer getMaxPanels() {
    return 25;
  }

  @Override
  protected Class<BetaDivScatterplotSpec> getVisualizationSpecClass() {
    return BetaDivScatterplotSpec.class;
  }

  @Override
  public ConstraintSpec getConstraintSpec() {
    return new ConstraintSpec()
      .dependencyOrder("yAxisVariable", "xAxisVariable", "overlayVariable", "facetVariable")
      .pattern()
//        .element("yAxisVariable") // TODO Remove
//          .shapes(APIVariableDataShape.CONTINUOUS) // TODO Remove
//        .element("xAxisVariable")
//          .shapes(APIVariableDataShape.CONTINUOUS) // Also remove
        .element("overlayVariable")
          .shapes(APIVariableDataShape.BINARY, APIVariableDataShape.ORDINAL, APIVariableDataShape.CATEGORICAL)
          .maxValues(8)
      .done();
  }

  @Override
  protected void validateVisualizationSpec(ScatterplotSpec pluginSpec) throws ValidationException {
    validateInputs(new DataElementSet()
            .entity(pluginSpec.getOutputEntityId())
//            .var("xAxisVariable", pluginSpec.getXAxisVariable())
//            .var("yAxisVariable", pluginSpec.getYAxisVariable())
            .var("overlayVariable", pluginSpec.getOverlayVariable()));
//            .var("facetVariable", pluginSpec.getFacetVariable()));
  }

  @Override
  protected List<StreamSpec> getRequestedStreams(ScatterplotSpec pluginSpec) {
    return ListBuilder.asList(
            new StreamSpec(DEFAULT_SINGLE_STREAM_NAME, pluginSpec.getOutputEntityId())
//                    .addVar(pluginSpec.getXAxisVariable())
//                    .addVar(pluginSpec.getYAxisVariable())
                    .addVar(pluginSpec.getOverlayVariable()));
//                    .addVars(pluginSpec.getFacetVariable()));
  }

  @Override
  protected void writeResults(OutputStream out, Map<String, InputStream> dataStreams) throws IOException {
    ScatterplotSpec spec = getPluginSpec();
    String xVar = "Axis 1"; // Will need different solution later. For now hard coding.
    String yVar = "Axis 2";  // Will need different solution later. For now hard coding.
    String overlayVar = toColNameOrEmpty(spec.getOverlayVariable());
    String xVarType = "NUMBER";
    String yVarType = "NUMBER";
    String overlayType = getVariableType(spec.getOverlayVariable());
    String xVarShape = "CONTINUOUS";
    String yVarShape = "CONTINUOUS";
    String overlayShape = getVariableDataShape(spec.getOverlayVariable());
    String valueSpec = "raw";  // No trend lines
    String showMissingness = spec.getShowMissingness() != null ? spec.getShowMissingness().getValue() : "FALSE";

    if (yVarType.equals("DATE") && !valueSpec.equals("raw")) {
      LOG.error("Cannot calculate trend lines for y-axis date variables. The `valueSpec` property must be set to `raw`.");
    }

    useRConnectionWithRemoteFiles(dataStreams, connection -> {
      connection.voidEval("data <- fread('" + DEFAULT_SINGLE_STREAM_NAME + "', na.strings=c(''))");
      connection.voidEval("map <- data.frame("
              + "'plotRef'=c('xAxisVariable', "
              + "       'yAxisVariable', "
              + "       'overlayVariable', "
              + "       'facetVariable1', "
              + "       'facetVariable2'), "
              + "'id'=c('" + xVar + "'"
              + ", '" + yVar + "'"
              + ", '" + overlayVar + "'), "
              + "'dataType'=c('" + xVarType + "'"
              + ", '" + yVarType + "'"
              + ", '" + overlayType + "'), "
              + "'dataShape'=c('" + xVarShape + "'"
              + ", '" + yVarShape + "'"
              + ", '" + overlayShape + "'), stringsAsFactors=FALSE)");
      String outFile = connection.eval("plot.data::scattergl(data, map, '" + valueSpec + "', " + showMissingness + ")").asString();
      try (RFileInputStream response = connection.openFile(outFile)) {
        IoUtil.transferStream(out, response);
      }
      out.flush();
    }); 
  }
}
