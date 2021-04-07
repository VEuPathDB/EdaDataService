package org.veupathdb.service.eda.ds.plugin.pass;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import org.gusdb.fgputil.IoUtil;
import org.gusdb.fgputil.ListBuilder;
import org.gusdb.fgputil.validation.ValidationBundle;
import org.gusdb.fgputil.validation.ValidationBundle.ValidationBundleBuilder;
import org.gusdb.fgputil.validation.ValidationException;
import org.gusdb.fgputil.validation.ValidationLevel;
import org.json.JSONObject;
import org.rosuda.REngine.Rserve.RFileInputStream;
import org.veupathdb.service.eda.common.model.EntityDef;
import org.veupathdb.service.eda.common.client.spec.StreamSpec;
import org.veupathdb.service.eda.common.model.ReferenceMetadata;
import org.veupathdb.service.eda.ds.constraints.ConstraintSpec;
import org.veupathdb.service.eda.ds.constraints.DataElementSet;
import org.veupathdb.service.eda.ds.plugin.AbstractPlugin;
import org.veupathdb.service.eda.generated.model.APIVariableDataShape;
import org.veupathdb.service.eda.generated.model.APIVariableType;
import org.veupathdb.service.eda.generated.model.ScatterplotPostRequest;
import org.veupathdb.service.eda.generated.model.ScatterplotSpec;
import org.veupathdb.service.eda.generated.model.VariableSpec;

import static org.veupathdb.service.eda.ds.util.RServeClient.useRConnectionWithRemoteFiles;

public class ScatterplotPlugin extends AbstractPlugin<ScatterplotPostRequest, ScatterplotSpec> {

  @Override
  public String getDisplayName() {
    return "Scatter plot";
  }

  @Override
  public String getDescription() {
    return "Visualize the relationship between two continuous variables";
  }

  @Override
  protected Class<ScatterplotSpec> getVisualizationSpecClass() {
    return ScatterplotSpec.class;
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
  protected void validateVisualizationSpec(ScatterplotSpec pluginSpec) throws ValidationException {
    validateInputs(new DataElementSet()
      .entity(pluginSpec.getOutputEntityId())
      .var("xAxisVariable", pluginSpec.getXAxisVariable())
      .var("yAxisVariable", pluginSpec.getYAxisVariable())
      .var("overlayVariable", pluginSpec.getOverlayVariable())
      .var("facetVariable", pluginSpec.getFacetVariable()));
  }

  @Override
  protected List<StreamSpec> getRequestedStreams(ScatterplotSpec pluginSpec) {
    return ListBuilder.asList(
      new StreamSpec(DEFAULT_SINGLE_STREAM_NAME, pluginSpec.getOutputEntityId())
        .addVar(pluginSpec.getXAxisVariable())
        .addVar(pluginSpec.getYAxisVariable())
        .addVar(pluginSpec.getOverlayVariable())
        .addVars(pluginSpec.getFacetVariable()));
  }

  @Override
  protected void writeResults(OutputStream out, Map<String, InputStream> dataStreams) throws IOException {
    ScatterplotSpec spec = getPluginSpec();
    
    boolean simpleScatter = true;
    if (spec.getFacetVariable() != null
         || !spec.getSmoothedMean().equals(ScatterplotSpec.SmoothedMeanType.FALSE)
         || dataStreams.size() != 1) {
      simpleScatter = false;
    }
    //for testing rserve
    simpleScatter = false;
    
    String xVar = toColNameOrEmpty(spec.getXAxisVariable());
    String yVar = toColNameOrEmpty(spec.getYAxisVariable());
    String overlayVar = toColNameOrEmpty(spec.getOverlayVariable());

    if (simpleScatter) {
      EntityDef entity = getReferenceMetadata().getEntity(spec.getOutputEntityId());
      Scanner s = new Scanner(dataStreams.get(DEFAULT_SINGLE_STREAM_NAME)).useDelimiter("\n");
      String[] header = s.nextLine().split("\t");

      int xVarIndex = 0;
      int yVarIndex = 1;
      Integer overlayVarIndex = null;
      for (int i = 0; i < header.length; i++) {
        if (Array.get(header, i).equals(overlayVar)) {
          overlayVarIndex = i;
        } else if (Array.get(header, i).equals(xVar)) {
          xVarIndex = i;
        } else if (Array.get(header, i).equals(yVar)) {
          yVarIndex = i;
        }
      }
      
      while(s.hasNextLine()) {
        String[] row = s.nextLine().split("\t");
        JSONObject scatterRow = new JSONObject();
        String xValue = row[xVarIndex];
        String yValue = row[yVarIndex];
        if (overlayVarIndex != null) {
          String currentGroup = row[overlayVarIndex];
          scatterRow.put("group", currentGroup);
        }
        scatterRow.put("seriesX", xValue); 
        scatterRow.put("seriesY", yValue);
        out.write(scatterRow.toString().getBytes());
      }
      
      s.close();
      out.flush();
    } else {
      EntityDef entity = getReferenceMetadata().getEntity(spec.getOutputEntityId());
      String facetVar1 = spec.getFacetVariable() != null ? toColNameOrEmpty(spec.getFacetVariable().get(0)) : "";
      String facetVar2 = spec.getFacetVariable() != null ? toColNameOrEmpty(spec.getFacetVariable().get(1)) : "";
      String xVarEntity = spec.getXAxisVariable() != null ? spec.getXAxisVariable().getEntityId() : "";
      String yVarEntity = spec.getYAxisVariable() != null ? spec.getYAxisVariable().getEntityId() : "";
      String overlayEntity = spec.getOverlayVariable() != null ? spec.getOverlayVariable().getEntityId() : "";
      String facetEntity1 = spec.getFacetVariable() != null ? spec.getFacetVariable().get(0).getEntityId() : "";
      String facetEntity2 = spec.getFacetVariable() != null ? spec.getFacetVariable().get(1).getEntityId() : "";
      String xVarType = spec.getXAxisVariable() != null ? entity.getVariable(spec.getXAxisVariable()).getType().toString() : "";
      String yVarType = spec.getYAxisVariable() != null ? entity.getVariable(spec.getYAxisVariable()).getType().toString() : "";
      String overlayType = spec.getOverlayVariable() != null ? entity.getVariable(spec.getOverlayVariable()).getType().toString() : "";
      String facetType1 = spec.getFacetVariable() != null ? entity.getVariable(spec.getFacetVariable().get(0)).getType().toString() : "";
      String facetType2 = spec.getFacetVariable() != null ? entity.getVariable(spec.getFacetVariable().get(1)).getType().toString() : "";
      String smoothedMean = spec.getSmoothedMean().equals(ScatterplotSpec.SmoothedMeanType.FALSE) ? "raw" : "smoothedMean";
      
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
        String outFile = connection.eval("scattergl(data, map, '" + smoothedMean + "')").asString();
        try (RFileInputStream response = connection.openFile(outFile)) {
          IoUtil.transferStream(out, response);
        }
        out.flush();
      }); 
    }
  }
}
