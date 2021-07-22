package org.veupathdb.service.eda.ds.plugin.pass;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import org.gusdb.fgputil.IoUtil;
import org.gusdb.fgputil.ListBuilder;
import org.gusdb.fgputil.validation.ValidationBundle;
import org.gusdb.fgputil.validation.ValidationException;
import org.json.JSONObject;
import org.rosuda.REngine.Rserve.RFileInputStream;
import org.veupathdb.service.eda.common.client.spec.StreamSpec;
import org.veupathdb.service.eda.common.model.EntityDef;
import org.veupathdb.service.eda.ds.constraints.ConstraintSpec;
import org.veupathdb.service.eda.ds.constraints.DataElementSet;
import org.veupathdb.service.eda.ds.plugin.AbstractPlugin;
import org.veupathdb.service.eda.generated.model.APIVariableDataShape;
import org.veupathdb.service.eda.generated.model.BarplotPostRequest;
import org.veupathdb.service.eda.generated.model.BarplotSpec;

import static org.veupathdb.service.eda.ds.util.RServeClient.useRConnectionWithRemoteFiles;

public class BarplotPlugin extends AbstractPlugin<BarplotPostRequest, BarplotSpec> {

  @Override
  public String getDisplayName() {
    return "Bar plot";
  }

  @Override
  public String getDescription() {
    return "Visualize the distribution of a categorical variable";
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
  protected Class<BarplotSpec> getVisualizationSpecClass() {
    return BarplotSpec.class;
  }

  @Override
  public ConstraintSpec getConstraintSpec() {
    return new ConstraintSpec()
      .dependencyOrder("xAxisVariable", "overlayVariable")
      .pattern()
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
  protected void validateVisualizationSpec(BarplotSpec pluginSpec) throws ValidationException {
    validateInputs(new DataElementSet()
      .entity(pluginSpec.getOutputEntityId())
      .var("xAxisVariable", pluginSpec.getXAxisVariable())
      .var("overlayVariable", pluginSpec.getOverlayVariable())
      .var("facetVariable", pluginSpec.getFacetVariable()));
    if (pluginSpec.getBarmode() == null) {
      throw new ValidationException("barmode is a required property");
    }
  }

  @Override
  protected List<StreamSpec> getRequestedStreams(BarplotSpec pluginSpec) {
    return ListBuilder.asList(
      new StreamSpec(DEFAULT_SINGLE_STREAM_NAME, pluginSpec.getOutputEntityId())
        .addVar(pluginSpec.getXAxisVariable())
        .addVar(pluginSpec.getOverlayVariable())
        .addVars(pluginSpec.getFacetVariable()));
  }

  @Override
  protected void writeResults(OutputStream out, Map<String, InputStream> dataStreams) throws IOException {
    BarplotSpec spec = getPluginSpec();
    
    boolean simpleBar = true;
    // TODO consider adding facets to simpleBar ?
    if (spec.getFacetVariable() != null
         || !spec.getValueSpec().getValue().equals("count")
         || dataStreams.size() != 1) {
      simpleBar = false;
    }
    
    //until i figure out the sort issue
    simpleBar = false;
    if (simpleBar) {
      int rowCount = 0;
      Scanner s = new Scanner(dataStreams.get(DEFAULT_SINGLE_STREAM_NAME)).useDelimiter("\n");
      
      Integer groupVarIndex = null;
      int xVarIndex = 0;
      String xVar = toColNameOrEmpty(getReferenceMetadata()
          .getVariable(spec.getXAxisVariable()).orElseThrow());
      if (spec.getOverlayVariable() != null) {
        String groupVar = toColNameOrEmpty(spec.getOverlayVariable());
        // expect two cols ordered by overlayVar and then xAxisVar
        // TODO will be ordered by entity id
        String[] header = s.nextLine().split("\t");
        groupVarIndex = 0;
        xVarIndex = 1;
        if (!header[0].equals(groupVar)) {
          groupVarIndex = 1;
          xVarIndex = 0;
        }
      }

      // read the header
      String[] row = s.nextLine().split("\t");
      String currentXCategory = row[xVarIndex];
      String currentGroup = null;
      if (groupVarIndex != null) {
        currentGroup = row[groupVarIndex];
      }
      rowCount = 1;
  
      while(s.hasNextLine()) {
        row = s.nextLine().split("\t");
        String xCategory = row[xVarIndex];
        String group = null;
        boolean increment = false;
        if (groupVarIndex != null) {
          group = row[groupVarIndex];
          if (group.equals(currentGroup) && xCategory.equals(currentXCategory)) {
            increment = true;
          }
        } else {
          if (xCategory.equals(currentXCategory)) {
            increment = true;
          }
        }
        if (increment) {
          rowCount++;
        } else {
          JSONObject barRow = new JSONObject();
          if (currentGroup != null) {
            barRow.put("group", currentGroup);
          }
          barRow.put("label", currentXCategory); 
          barRow.put("value", rowCount);
          out.write(barRow.toString().getBytes());
          currentGroup = group;
          currentXCategory = xCategory;
          rowCount = 1;
        }
      }
      
      s.close();
      out.flush();
    }
    else {
      String xVar = toColNameOrEmpty(spec.getXAxisVariable());
      String overlayVar = toColNameOrEmpty(spec.getOverlayVariable());
      String facetVar1 = toColNameOrEmpty(spec.getFacetVariable(), 0);
      String facetVar2 = toColNameOrEmpty(spec.getFacetVariable(), 1);
      String xVarType = getVariableType(spec.getXAxisVariable());
      String overlayType = getVariableType(spec.getOverlayVariable());
      String facetType1 = getVariableType(spec.getFacetVariable(), 0);
      String facetType2 = getVariableType(spec.getFacetVariable(), 1);
      String xVarShape = getVariableDataShape(spec.getXAxisVariable());
      String overlayShape = getVariableDataShape(spec.getOverlayVariable());
      String facetShape1 = getVariableDataShape(spec.getFacetVariable(), 0);
      String facetShape2 = getVariableDataShape(spec.getFacetVariable(), 1);
      String showMissingness = spec.getShowMissingness() != null ? spec.getShowMissingness().getValue() : "FALSE";
      String barmode = spec.getBarmode().getValue();
      
      useRConnectionWithRemoteFiles(dataStreams, connection -> {
        connection.voidEval("data <- fread('" + DEFAULT_SINGLE_STREAM_NAME + "', na.strings=c(''))");
        String createMapString = "map <- data.frame("
            + "'plotRef'=c('xAxisVariable', "
            + "       'overlayVariable', "
            + "       'facetVariable1', "
            + "       'facetVariable2'), "
            + "'id'=c('" + xVar + "'"
            + ", '" + overlayVar + "'"
            + ", '" + facetVar1 + "'"
            + ", '" + facetVar2 + "'), "
            + "'dataType'=c('" + xVarType + "'"
            + ", '" + overlayType + "'"
            + ", '" + facetType1 + "'"
            + ", '" + facetType2 + "'), "
            + "'dataShape'=c('" + xVarShape + "'"
            + ", '" + overlayShape + "'"
            + ", '" + facetShape1 + "'"
            + ", '" + facetShape2 + "'), stringsAsFactors=FALSE)";
        connection.voidEval(createMapString);
        String outFile = connection.eval("plot.data::bar(data, map, '" + 
                                                         spec.getValueSpec().getValue() + "', '" + 
                                                         barmode + "', " + 
                                                         showMissingness + ")").asString();
        try (RFileInputStream response = connection.openFile(outFile)) {
          IoUtil.transferStream(out, response);
        }
        out.flush();
      });
    }
  }
}
