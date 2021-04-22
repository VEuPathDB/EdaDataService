package org.veupathdb.service.eda.ds.plugin.pass;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import org.gusdb.fgputil.IoUtil;
import org.gusdb.fgputil.ListBuilder;
import org.gusdb.fgputil.validation.ValidationBundle;
import org.gusdb.fgputil.validation.ValidationBundle.ValidationBundleBuilder;
import org.gusdb.fgputil.validation.ValidationException;
import org.gusdb.fgputil.validation.ValidationLevel;
import org.rosuda.REngine.Rserve.RFileInputStream;
import org.veupathdb.service.eda.common.client.spec.StreamSpec;
import org.veupathdb.service.eda.common.model.ReferenceMetadata;
import org.veupathdb.service.eda.ds.constraints.ConstraintSpec;
import org.veupathdb.service.eda.ds.constraints.DataElementSet;
import org.veupathdb.service.eda.ds.plugin.AbstractPlugin;
import org.veupathdb.service.eda.generated.model.APIVariableDataShape;
import org.veupathdb.service.eda.generated.model.APIVariableType;
import org.veupathdb.service.eda.generated.model.LineplotPostRequest;
import org.veupathdb.service.eda.generated.model.LineplotSpec;
import org.veupathdb.service.eda.generated.model.VariableSpec;

import static org.veupathdb.service.eda.ds.util.RServeClient.useRConnectionWithRemoteFiles;

public class LineplotPlugin extends AbstractPlugin<LineplotPostRequest, LineplotSpec> {

  @Override
  public String getDisplayName() {
    return "Line plot";
  }

  @Override
  public String getDescription() {
    return "Visualize the relationship between two continuous variables";
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
          .shapes(APIVariableDataShape.CONTINUOUS)
        .element("xAxisVariable")
          .shapes(APIVariableDataShape.CONTINUOUS, APIVariableDataShape.ORDINAL)
        .element("overlayVariable")
          .shapes(APIVariableDataShape.BINARY, APIVariableDataShape.ORDINAL, APIVariableDataShape.CATEGORICAL)
        .element("facetVariable")
          .required(false)
          .max(2)
          .shapes(APIVariableDataShape.BINARY, APIVariableDataShape.ORDINAL, APIVariableDataShape.CATEGORICAL)
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
    String xVar = toColNameOrEmpty(spec.getXAxisVariable());
    String yVar = toColNameOrEmpty(spec.getYAxisVariable());
    String overlayVar = toColNameOrEmpty(spec.getOverlayVariable());
    String facetVar1 = toColNameOrEmpty(spec.getFacetVariable(), 0);
    String facetVar2 = toColNameOrEmpty(spec.getFacetVariable(), 1);
    String xVarEntity = getVariableEntityId(spec.getXAxisVariable());
    String yVarEntity = getVariableEntityId(spec.getYAxisVariable());
    String overlayEntity = getVariableEntityId(spec.getOverlayVariable());
    String facetEntity1 = getVariableEntityId(spec.getFacetVariable(), 0);
    String facetEntity2 = getVariableEntityId(spec.getFacetVariable(), 1);
    String xVarType = getVariableType(spec.getXAxisVariable());
    String yVarType = getVariableType(spec.getYAxisVariable());
    String overlayType = getVariableType(spec.getOverlayVariable());
    String facetType1 = getVariableType(spec.getFacetVariable(), 0);
    String facetType2 = getVariableType(spec.getFacetVariable(), 1);
    
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
            + ", '" + facetType2 + "'), stringsAsFactors=FALSE)");
      String outFile = connection.eval("scattergl(data, map, 'raw')").asString();
      try (RFileInputStream response = connection.openFile(outFile)) {
        IoUtil.transferStream(out, response);
      }
      out.flush();
    }); 
  }
}
