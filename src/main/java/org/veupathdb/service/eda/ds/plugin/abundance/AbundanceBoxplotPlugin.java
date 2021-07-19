package org.veupathdb.service.eda.ds.plugin.abundance;

import org.gusdb.fgputil.IoUtil;
import org.gusdb.fgputil.ListBuilder;
import org.gusdb.fgputil.validation.ValidationException;
import org.rosuda.REngine.Rserve.RFileInputStream;
import org.veupathdb.service.eda.common.client.spec.StreamSpec;
import org.veupathdb.service.eda.ds.constraints.ConstraintSpec;
import org.veupathdb.service.eda.ds.constraints.DataElementSet;
import org.veupathdb.service.eda.ds.plugin.AbstractPlugin;
import org.veupathdb.service.eda.generated.model.APIVariableDataShape;
import org.veupathdb.service.eda.generated.model.AbundanceBoxplotPostRequest;
import org.veupathdb.service.eda.generated.model.AbundanceBoxplotSpec;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.veupathdb.service.eda.ds.util.RServeClient.useRConnectionWithRemoteFiles;

public class AbundanceBoxplotPlugin extends AbstractPlugin<AbundanceBoxplotPostRequest, AbundanceBoxplotSpec> {

  @Override
  public String getDisplayName() {
    return "Box plot";
  }

  @Override
  public String getDescription() {
    return "Visualize summary values for OTU abundance";
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
  protected void validateVisualizationSpec(AbundanceBoxplotSpec pluginSpec) throws ValidationException {
    validateInputs(new DataElementSet()
      .entity(pluginSpec.getOutputEntityId())
      .var("xAxisVariable", pluginSpec.getXAxisVariable())
//      .var("yAxisVariable", pluginSpec.getYAxisVariable())  // Also not needed. Assuming compute service will validate
      .var("overlayVariable", pluginSpec.getOverlayVariable())
      .var("facetVariable", pluginSpec.getFacetVariable()));
  }

  @Override
  protected List<StreamSpec> getRequestedStreams(AbundanceBoxplotSpec pluginSpec) {
    return ListBuilder.asList(
      new StreamSpec(DEFAULT_SINGLE_STREAM_NAME, pluginSpec.getOutputEntityId())
        .addVar(pluginSpec.getXAxisVariable())
//        .addVar(pluginSpec.getYAxisVariable())  // Probably won't need or will need something different
        .addVar(pluginSpec.getOverlayVariable())
        .addVars(pluginSpec.getFacetVariable()));
  }

  @Override
  protected void writeResults(OutputStream out, Map<String, InputStream> dataStreams) throws IOException {
    AbundanceBoxplotSpec spec = getPluginSpec();
    String xVar = toColNameOrEmpty(spec.getXAxisVariable());
    String yVar = "Abundance"; // To do - needs to come from CS because could be one taxa or many
    String overlayVar = toColNameOrEmpty(spec.getOverlayVariable());
    String facetVar1 = toColNameOrEmpty(spec.getFacetVariable(), 0);
    String facetVar2 = toColNameOrEmpty(spec.getFacetVariable(), 1);
    String xVarEntity = getVariableEntityId(spec.getXAxisVariable());
    String yVarEntity = "Assay";
    String overlayEntity = getVariableEntityId(spec.getOverlayVariable());
    String facetEntity1 = getVariableEntityId(spec.getFacetVariable(), 0);
    String facetEntity2 = getVariableEntityId(spec.getFacetVariable(), 1);
    String xVarType = getVariableType(spec.getXAxisVariable());
    String yVarType = "NUMBER"; // we can hard code these
    String overlayType = getVariableType(spec.getOverlayVariable());
    String facetType1 = getVariableType(spec.getFacetVariable(), 0);
    String facetType2 = getVariableType(spec.getFacetVariable(), 1);
    String xVarShape = getVariableDataShape(spec.getXAxisVariable());
    String yVarShape = "CONTINUOUS"; // we can hard code these
    String overlayShape = getVariableDataShape(spec.getOverlayVariable());
    String facetShape1 = getVariableDataShape(spec.getFacetVariable(), 0);
    String facetShape2 = getVariableDataShape(spec.getFacetVariable(), 1);

    // Proposed solution for listvars, using x var as example (see toCommaSeparated in AbstractPlugin.java)
    // String xVars = toCommaSeparated(toColsNamesOrEmpty(sepc.getListVariable()));
    // String xVarEntities = toCommaSeparated(getListVarEntityIds(spec.getListVariable()));
    // String xVarTypes = toCommaSeparated(getListVarType(spec.getListVariable()));
    // String xVarShapes = toCommaSeparated(getListVarShape(spec.getListVariable()));
    // String[] xVarPlotRefs = new boolean[5];
    // Arrays.fill(array, "xAxisVariable");
    // String xVarPlotRef = toCommaSeparated(xVarPlotRefs);

    // repeat above for all variables. 


    useRConnectionWithRemoteFiles(dataStreams, connection -> {
      connection.voidEval("data <- fread('" + DEFAULT_SINGLE_STREAM_NAME + "', na.strings=c(''))");
      connection.voidEval("map <- data.frame("
//          + "'plotRef'=c(" + xVarPlotRef + ", "
          + "       'yAxisVariable', "
          + "       'overlayVariable', "
          + "       'facetVariable1', "
          + "       'facetVariable2'), "
          + "'id'=c('" + xVar + "'"
          + ", '" + yVar + "'"
          + ", '" + overlayVar + "'"
          + ", '" + facetVar1 + "'"
          + ", '" + facetVar2 + "'), "
          + "'entityId'=c('" + xVarEntity + "'"
          + ", '" + yVarEntity + "'"
          + ", '" + overlayEntity + "'"
          + ", '" + facetEntity1 + "'"
          + ", '" + facetEntity2 + "'), "
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
      String outFile = connection.eval("plot.data::box(data, map, '" +
          spec.getPoints().toString().toLowerCase() + "', '" +
          spec.getMean().toString().toUpperCase() + "')").asString();
      try (RFileInputStream response = connection.openFile(outFile)) {
        IoUtil.transferStream(out, response);
      }
      out.flush();
    });
  }
}
