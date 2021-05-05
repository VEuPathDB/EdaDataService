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
import org.veupathdb.service.eda.common.model.EntityDef;
import org.veupathdb.service.eda.common.client.spec.StreamSpec;
import org.veupathdb.service.eda.common.model.ReferenceMetadata;
import org.veupathdb.service.eda.ds.constraints.ConstraintSpec;
import org.veupathdb.service.eda.ds.constraints.DataElementSet;
import org.veupathdb.service.eda.ds.plugin.AbstractPlugin;
import org.veupathdb.service.eda.generated.model.APIVariableDataShape;
import org.veupathdb.service.eda.generated.model.APIVariableType;
import org.veupathdb.service.eda.generated.model.HeatmapPostRequest;
import org.veupathdb.service.eda.generated.model.HeatmapSpec;
import org.veupathdb.service.eda.generated.model.VariableSpec;

import static org.veupathdb.service.eda.ds.util.RServeClient.useRConnectionWithRemoteFiles;

public class HeatmapPlugin extends AbstractPlugin<HeatmapPostRequest, HeatmapSpec> {

  @Override
  public String getDisplayName() {
    return "Heatmap";
  }

  @Override
  public String getDescription() {
    return "Visualize the magnitude of a continuous numeric variable";
  }

  @Override
  protected Class<HeatmapSpec> getVisualizationSpecClass() {
    return HeatmapSpec.class;
  }

  @Override
  public ConstraintSpec getConstraintSpec() {
    return new ConstraintSpec()
      .dependencyOrder("zAxisVariable", "yAxisVariable", "xAxisVariable", "facetVariable")
      .pattern()
        .element("zAxisVariable")
          .types(APIVariableType.NUMBER)
          .shapes(APIVariableDataShape.CONTINUOUS)
        .element("yAxisVariable")
          .shapes(APIVariableDataShape.BINARY, APIVariableDataShape.ORDINAL, APIVariableDataShape.CATEGORICAL)
        .element("xAxisVariable")
          .shapes(APIVariableDataShape.BINARY, APIVariableDataShape.ORDINAL, APIVariableDataShape.CATEGORICAL)
        .element("facetVariable")
          .required(false)
          .max(2)
          .shapes(APIVariableDataShape.BINARY, APIVariableDataShape.ORDINAL, APIVariableDataShape.CATEGORICAL)
      .done();
  }
  
  @Override
  protected void validateVisualizationSpec(HeatmapSpec pluginSpec) throws ValidationException {
    validateInputs(new DataElementSet()
      .entity(pluginSpec.getOutputEntityId())
      .var("xAxisVariable", pluginSpec.getXAxisVariable())
      .var("yAxisVariable", pluginSpec.getYAxisVariable())
      .var("zAxisVariable", pluginSpec.getZAxisVariable())
      .var("facetVariable", pluginSpec.getFacetVariable()));
    // z-axis not optional if value spec is series
    if (pluginSpec.getValueSpec().equals(HeatmapSpec.ValueSpecType.SERIES) && pluginSpec.getZAxisVariable() == null) {
      throw new ValidationException("zAxisVariable required for heatmap of type 'series'.");
    }
  }

  @Override
  protected List<StreamSpec> getRequestedStreams(HeatmapSpec pluginSpec) {
    return ListBuilder.asList(
      new StreamSpec(DEFAULT_SINGLE_STREAM_NAME, pluginSpec.getOutputEntityId())
        .addVar(pluginSpec.getXAxisVariable())
        .addVar(pluginSpec.getYAxisVariable())
        .addVar(pluginSpec.getZAxisVariable())
        .addVars(pluginSpec.getFacetVariable()));
  }

  @Override
  protected void writeResults(OutputStream out, Map<String, InputStream> dataStreams) throws IOException {
    useRConnectionWithRemoteFiles(dataStreams, connection -> {
      HeatmapSpec spec = getPluginSpec();
      connection.voidEval("data <- fread('" + DEFAULT_SINGLE_STREAM_NAME + "')");
      connection.voidEval("map <- data.frame("
          + "'plotRef'=c('xAxisVariable', "
          + "       'yAxisVariable', "
          + "       'zAxisVariable', "
          + "       'facetVariable1', "
          + "       'facetVariable2'), "
          + "'id'=c('" + toColNameOrEmpty(spec.getXAxisVariable()) + "'"
          + ", '" + toColNameOrEmpty(spec.getYAxisVariable()) + "'"
          + ", '" + toColNameOrEmpty(spec.getZAxisVariable()) + "'"
          + ", '" + toColNameOrEmpty(spec.getFacetVariable().get(0)) + "'"
          + ", '" + toColNameOrEmpty(spec.getFacetVariable().get(1)) + "'), stringsAsFactors=FALSE)");
      String outFile = connection.eval("plot.data::heatmap(data, map, '" + spec.getValueSpec().toString().toLowerCase() + "')").asString();
      try (RFileInputStream response = connection.openFile(outFile)) {
        IoUtil.transferStream(out, response);
      }
      out.flush();
    });
  }
}