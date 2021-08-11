package org.veupathdb.service.eda.ds.plugin.pass;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gusdb.fgputil.IoUtil;
import org.gusdb.fgputil.ListBuilder;
import org.gusdb.fgputil.validation.ValidationBundle;
import org.gusdb.fgputil.validation.ValidationBundle.ValidationBundleBuilder;
import org.gusdb.fgputil.validation.ValidationException;
import org.gusdb.fgputil.validation.ValidationLevel;
import org.rosuda.REngine.Rserve.RFileInputStream;
import org.veupathdb.service.eda.common.model.EntityDef;
import org.veupathdb.service.eda.common.model.ReferenceMetadata;
import org.veupathdb.service.eda.ds.constraints.ConstraintSpec;
import org.veupathdb.service.eda.ds.constraints.DataElementSet;
import org.veupathdb.service.eda.ds.plugin.AbstractPlugin;
import org.veupathdb.service.eda.generated.model.APIVariableDataShape;
import org.veupathdb.service.eda.generated.model.APIVariableType;
import org.veupathdb.service.eda.generated.model.BinSpec;
import org.veupathdb.service.eda.generated.model.HistogramPostRequest;
import org.veupathdb.service.eda.generated.model.HistogramSpec;
import org.veupathdb.service.eda.generated.model.VariableSpec;
import org.veupathdb.service.eda.common.client.spec.StreamSpec;

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
    return Arrays.asList("ClinEpiDB", "MicrobiomeDB");
  }
  
  @Override
  public Integer getMaxPanels() {
    return 25;
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
          .shapes(APIVariableDataShape.CONTINUOUS)
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

    String overlayType = getVariableType(spec.getOverlayVariable());
    String facetType1 = getVariableType(spec.getFacetVariable(), 0);
    String facetType2 = getVariableType(spec.getFacetVariable(), 1);
    String xVarShape = getVariableDataShape(spec.getXAxisVariable());
    String overlayShape = getVariableDataShape(spec.getOverlayVariable());
    String facetShape1 = getVariableDataShape(spec.getFacetVariable(), 0);
    String facetShape2 = getVariableDataShape(spec.getFacetVariable(), 1);
    String showMissingness = spec.getShowMissingness() != null ? spec.getShowMissingness().getValue() : "FALSE";
    String barMode = spec.getBarMode().getValue();
    
    useRConnectionWithRemoteFiles(dataStreams, connection -> {
      connection.voidEval(getVoidEvalFreadCommand(DEFAULT_SINGLE_STREAM_NAME, 
          spec.getXAxisVariable(),
          spec.getOverlayVariable(),
          getVariableSpecFromList(spec.getFacetVariable(), 0),
          getVariableSpecFromList(spec.getFacetVariable(), 1)));
      connection.voidEval(getVoidEvalVarMetadataMap(varMap));
      
      if (spec.getViewport() != null) {
        // think if we just pass the string plot.data will convert it to the claimed type
        if (xVarType.equals("NUMBER")) {
          connection.voidEval("viewport <- list('xMin'=" + spec.getViewport().getXMin() + ", 'xMax'=" + spec.getViewport().getXMax() + ")");
        } else {
          connection.voidEval("viewport <- list('xMin'=" + singleQuote(spec.getViewport().getXMin()) + ", 'xMax'=" + singleQuote(spec.getViewport().getXMax()) + ")");
        }
      } else {
        connection.voidEval("viewport <- NULL");
      }
      
      BinSpec binSpec = spec.getBinSpec();
      String binReportValue = binSpec.getType().getValue();
      
      //consider reorganizing conditions, move check for null value up a level ?
      if (binReportValue.equals("numBins")) {
        if (binSpec.getValue() != null) {
          String numBins = binSpec.getValue().toString();
          connection.voidEval("xVP <- adjustToViewport(data$" + xVar + ", viewport)");
          if (xVarType.equals("NUMBER")) {
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
        if (xVarType.equals("NUMBER")) {
          binWidth = binSpec.getValue() == null ? "NULL" : "as.numeric('" + binSpec.getValue() + "')";
          if (binSpec.getUnits() != null) {
            LOG.warn("The `units` property of the `BinSpec` class is only used for DATE x-axis variables. It will be ignored.");
          }
        } else {
          binWidth = binSpec.getValue() == null ? "NULL" : "'" + binSpec.getValue().toString() + " " + binSpec.getUnits().toString().toLowerCase() + "'";
        }
        connection.voidEval("binWidth <- " + binWidth);
      }
      
      String outFile = connection.eval("plot.data::histogram(data, map, binWidth, '" + 
                                                             spec.getValueSpec().getValue() + "', '" + 
                                                             binReportValue + "', '" + 
                                                             barMode + "', viewport, " +
                                                             showMissingness + ")").asString();
      try (RFileInputStream response = connection.openFile(outFile)) {
        IoUtil.transferStream(out, response);
      }
      out.flush();
    });
  }
}
