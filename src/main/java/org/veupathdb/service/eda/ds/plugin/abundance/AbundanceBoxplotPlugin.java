package org.veupathdb.service.eda.ds.plugin.abundance;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.gusdb.fgputil.IoUtil;
import org.gusdb.fgputil.ListBuilder;
import org.gusdb.fgputil.validation.ValidationException;
import org.rosuda.REngine.Rserve.RFileInputStream;
import org.veupathdb.service.eda.common.client.spec.StreamSpec;
import org.veupathdb.service.eda.ds.constraints.ConstraintSpec;
import org.veupathdb.service.eda.ds.constraints.DataElementSet;
import org.veupathdb.service.eda.ds.plugin.AbstractPlugin;
import org.veupathdb.service.eda.generated.model.*;

import static org.veupathdb.service.eda.ds.util.RServeClient.useRConnectionWithRemoteFiles;

public class AbundanceBoxplotPlugin extends AbstractPlugin<AbundanceBoxplotPostRequest, AbundanceBoxplotSpec> {

  @Override
  public String getDisplayName() {
    return "Box plot";
  }

  @Override
  public String getDescription() {
    return "Visualize summary values for abundance data";
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
  protected Class<AbundanceBoxplotSpec> getVisualizationSpecClass() {
    return AbundanceBoxplotSpec.class;
  }

  @Override
  public ConstraintSpec getConstraintSpec() {
    return new ConstraintSpec()
      .dependencyOrder("yAxisVariable", "xAxisVariable", "overlayVariable", "facetVariable")
      .pattern()
//        .element("yAxisVariable") // Not needed because we won't have y-axis dropdown
//          .types(APIVariableType.NUMBER)  // Can remove
        .element("xAxisVariable")
          .shapes(APIVariableDataShape.BINARY, APIVariableDataShape.ORDINAL, APIVariableDataShape.CATEGORICAL)
          .maxValues(10)
        .element("overlayVariable")
          .shapes(APIVariableDataShape.BINARY, APIVariableDataShape.ORDINAL, APIVariableDataShape.CATEGORICAL)
          .maxValues(8)  
        .element("facetVariable")
          .required(false)
          .maxVars(2)
          .shapes(APIVariableDataShape.BINARY, APIVariableDataShape.ORDINAL, APIVariableDataShape.CATEGORICAL)
      .done();
  }

  @Override
  protected void validateVisualizationSpec(BoxplotSpec pluginSpec) throws ValidationException {
    validateInputs(new DataElementSet()
            .entity(pluginSpec.getOutputEntityId())
            .var("xAxisVariable", pluginSpec.getXAxisVariable())
//            .var("yAxisVariable", pluginSpec.getYAxisVariable())
            .var("overlayVariable", pluginSpec.getOverlayVariable())
            .var("facetVariable", pluginSpec.getFacetVariable()));
  }

  @Override
  protected List<StreamSpec> getRequestedStreams(BoxplotSpec pluginSpec) {
    return ListBuilder.asList(
            new StreamSpec(DEFAULT_SINGLE_STREAM_NAME, pluginSpec.getOutputEntityId())
                    .addVar(pluginSpec.getXAxisVariable())
//                    .addVar(pluginSpec.getYAxisVariable())
                    .addVar(pluginSpec.getOverlayVariable())
                    .addVars(pluginSpec.getFacetVariable()));
  }

  @Override
  protected void writeResults(OutputStream out, Map<String, InputStream> dataStreams) throws IOException {
    AbundanceBoxplotSpec spec = getPluginSpec();
    String xVar = toColNameOrEmpty(spec.getXAxisVariable());
    String yVar = "Abundance";
    String overlayVar = toColNameOrEmpty(spec.getOverlayVariable());
    String facetVar1 = toColNameOrEmpty(spec.getFacetVariable(), 0);
    String facetVar2 = toColNameOrEmpty(spec.getFacetVariable(), 1);
    // String xVarDisplayName = getVariableDisplayName(spec.getXAxisVariable());
    // String yVarDisplayName = getVariableDisplayName(spec.getYAxisVariable());
    // String overlayVarDisplayName = getVariableDisplayName(spec.getOverlayVariable());
    // String facet1VarDisplayName = getVariableDisplayName(spec.getFacetVariable(), 0);
    // String facet2VarDisplayName = getVariableDisplayName(spec.getFacetVariable(), 1);
    String xVarType = getVariableType(spec.getXAxisVariable());
    String yVarType = "NUMBER";
    String overlayType = getVariableType(spec.getOverlayVariable());
    String facetType1 = getVariableType(spec.getFacetVariable(), 0);
    String facetType2 = getVariableType(spec.getFacetVariable(), 1);
    String xVarShape = getVariableDataShape(spec.getXAxisVariable());
    String yVarShape = "CONTINUOUS";
    String overlayShape = getVariableDataShape(spec.getOverlayVariable());
    String facetShape1 = getVariableDataShape(spec.getFacetVariable(), 0);
    String facetShape2 = getVariableDataShape(spec.getFacetVariable(), 1);
    String showMissingness = spec.getShowMissingness() != null ? spec.getShowMissingness().getValue() : "FALSE";
    String computeStats = spec.getComputeStats() != null ? spec.getComputeStats().getValue() : "TRUE";
    String showMean = spec.getMean() != null ? spec.getMean().getValue() : "FALSE";

    // Proposed solution for listvars, using x var as example (see toCommaSepString in AbstractPlugin.java)
    // String xVars = toCommaSepString(toColsNamesOrEmpty(sepc.getListVariable()));
    // String xVarDisplayNames = toCommaSepString(getVariableDisplayName(spec.getXAxisVariable()));
    // String xVarTypes = toCommaSepString(getListVarType(spec.getListVariable()));
    // String xVarShapes = toCommaSepString(getListVarShape(spec.getListVariable()));
    // String[] xVarPlotRefs = new boolean[5];
    // Arrays.fill(array, "xAxisVariable");
    // String xVarPlotRef = toCommaSepString(xVarPlotRefs);

    // repeat above for all variables.


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
              + ", '" + overlayVar + "'"
              + ", '" + facetVar1 + "'"
//              + "'displayName'=c('" + xVarDisplayName + "'"
//              + ", '" + yVarDisplayName + "'"
//              + ", '" + overlayVarDisplayName + "'"
//              + ", '" + facetVar1DisplayName + "'"
//              + ", '" + facetVar2DisplayName + "'), "
              + "'dataType'=c('" + xVarType + "'"
              + ", '" + yVarType + "'"
              + ", '" + overlayType + "'"
              + ", '" + facetType1 + "'"
              + ", '" + facetType2 + "'), "
              + "'dataShape'=c('" + xVarShape + "'"
              + ", '" + yVarShape + "'"
              + ", '" + overlayShape + "'"
              + ", '" + facetShape1 + "'"
              + ", '" + facetShape2 + "'), stringsAsFactors=FALSE)");
      connection.voidEval("head(data)");
      System.err.println("plot.data::box(data, map, '" +
              spec.getPoints().getValue() + "', " +
              showMean + ", " +
              computeStats + ", " +
              showMissingness + ")");
      String outFile = connection.eval("plot.data::box(data, map, '" +
              spec.getPoints().getValue() + "', " +
              showMean + ", " +
              computeStats + ", " +
              showMissingness + ")").asString();
      try (RFileInputStream response = connection.openFile(outFile)) {
        IoUtil.transferStream(out, response);
      }
      out.flush();
    });
  }
}
