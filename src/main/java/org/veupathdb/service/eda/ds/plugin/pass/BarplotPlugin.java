package org.veupathdb.service.eda.ds.plugin.pass;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import org.gusdb.fgputil.ListBuilder;
import org.gusdb.fgputil.validation.ValidationException;
import org.json.JSONObject;
import org.veupathdb.service.eda.common.client.spec.StreamSpec;
import org.veupathdb.service.eda.ds.constraints.ConstraintSpec;
import org.veupathdb.service.eda.ds.constraints.DataElementSet;
import org.veupathdb.service.eda.ds.plugin.AbstractPlugin;
import org.veupathdb.service.eda.generated.model.BarplotPostRequest;
import org.veupathdb.service.eda.generated.model.BarplotSpec;
import org.veupathdb.service.eda.generated.model.VariableSpec;

import static org.veupathdb.service.eda.ds.util.RServeClient.streamResult;
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
  protected Class<BarplotSpec> getVisualizationSpecClass() {
    return BarplotSpec.class;
  }

  @Override
  public ConstraintSpec getConstraintSpec() {
    return new ConstraintSpec()
      .dependencyOrder("xAxisVariable", "overlayVariable", "facetVariable")
      .pattern()
        .element("xAxisVariable")
          .maxValues(10)
          .description("Variable must have 10 or fewer unique values.")
        .element("overlayVariable")
          .maxValues(8)
          .description("Variable must have 8 or fewer unique values and be of the same or a parent entity as the X-axis variable.")
        .element("facetVariable")
          .required(false)
          .maxVars(2)
          .maxValues(7)
          .description("Variable(s) must have 25 or fewer cartesian products and be of the same or a parent entity of the Overlay variable.")
      .done();
  }

  @Override
  protected void validateVisualizationSpec(BarplotSpec pluginSpec) throws ValidationException {
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
      String showMissingness = spec.getShowMissingness() != null ? spec.getShowMissingness().getValue() : "FALSE";
      String barMode = spec.getBarMode().getValue();

      Map<String, VariableSpec> varMap = new HashMap<String, VariableSpec>();
      varMap.put("xAxisVariable", spec.getXAxisVariable());
      varMap.put("overlayVariable", spec.getOverlayVariable());
      varMap.put("facetVariable1", getVariableSpecFromList(spec.getFacetVariable(), 0));
      varMap.put("facetVariable2", getVariableSpecFromList(spec.getFacetVariable(), 1));
      
      useRConnectionWithRemoteFiles(dataStreams, connection -> {
        connection.voidEval(getVoidEvalFreadCommand(DEFAULT_SINGLE_STREAM_NAME, 
            spec.getXAxisVariable(),
            spec.getOverlayVariable(),
            getVariableSpecFromList(spec.getFacetVariable(), 0),
            getVariableSpecFromList(spec.getFacetVariable(), 1)));
        connection.voidEval(getVoidEvalVarMetadataMap(DEFAULT_SINGLE_STREAM_NAME, varMap));
        String cmd =
            "plot.data::bar(" + DEFAULT_SINGLE_STREAM_NAME + ", map, '" +
                spec.getValueSpec().getValue() + "', '" +
                barMode + "', " +
                showMissingness + ")";
        streamResult(connection, cmd, out);
      });
    }
  }
}
