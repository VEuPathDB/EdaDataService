package org.veupathdb.service.edads.plugin;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import org.gusdb.fgputil.ListBuilder;
import org.gusdb.fgputil.validation.ValidationBundle;
import org.gusdb.fgputil.validation.ValidationBundle.ValidationBundleBuilder;
import org.gusdb.fgputil.validation.ValidationException;
import org.gusdb.fgputil.validation.ValidationLevel;
import org.rosuda.REngine.REXP;
import org.veupathdb.service.edads.generated.model.APIVariableType;
import org.veupathdb.service.edads.generated.model.BarplotPostRequest;
import org.veupathdb.service.edads.generated.model.BarplotSpec;
import org.veupathdb.service.edads.util.AbstractEdadsPlugin;
import org.veupathdb.service.edads.util.EntityDef;
import org.veupathdb.service.edads.util.StreamSpec;

public class BarplotPlugin extends AbstractEdadsPlugin<BarplotPostRequest, BarplotSpec> {

  private static final String DATAFILE_NAME = "file1.txt";

  @Override
  protected Class<BarplotSpec> getAnalysisSpecClass() {
    return BarplotSpec.class;
  }

  @Override
  protected ValidationBundle validateAnalysisSpec(BarplotSpec pluginSpec) throws ValidationException {
    ValidationBundleBuilder validation = ValidationBundle.builder(ValidationLevel.RUNNABLE);
    EntityDef entity = getValidEntity(validation, pluginSpec.getEntityId());
    validateVariableNameAndType(validation, entity, "xAxisVariable", pluginSpec.getXAxisVariable(), APIVariableType.STRING);
    validateVariableNameAndType(validation, entity, "overlayVariable", pluginSpec.getOverlayVariable(), APIVariableType.STRING);
    for (String facetVar : pluginSpec.getFacetVariable()) {
      validateVariableName(validation, entity, "facetVariable", facetVar, APIVariableType.STRING);
    }
    return validation.build();
  }

  @Override
  protected List<StreamSpec> getRequestedStreams(BarplotSpec pluginSpec) {
    StreamSpec spec = new StreamSpec(DATAFILE_NAME, pluginSpec.getEntityId());
    spec.add(pluginSpec.getXAxisVariable());
    spec.add(pluginSpec.getOverlayVariable());
    spec.addAll(pluginSpec.getFacetVariable());
    return new ListBuilder<StreamSpec>(spec).toList();
  }

  @Override
  protected void writeResults(OutputStream out, Map<String, InputStream> dataStreams) throws IOException {
    BarplotSpec spec = getPluginSpec();
    
    boolean simpleBar = true;
    // TODO consider adding facets to simpleBar ?
    if (spec.getFacetVariable != null 
         || !spec.getValueSpec().equals('count')
         || dataStreams.size() != 1) {
      simpleBar = false;
    }
    
    if (simpleBar) {
      EntityDef entity = new EntityDef(spec.getEntityId());
      Wrapper<Integer> rowCount = new Wrapper<>(0);
      Scanner s = new Scanner(dataStreams.get(0)).useDelimiter("\n");
      
      int groupVarIndex = null;
      int xVarIndex = 0;
      String xVar = entity.get(spec.getXAxisVariable()).getId();
      if (spec.getOverlayVariable != null) {
        String groupVar = entity.get(spec.getOverlayVariable()).getId();
        // expect two cols ordered by overlayVar and then xAxisVar
        String[] header = s.nextLine().asString().split("\t");
        groupVarIndex = 0;
        xVarIndex = 1;
        if (!Array.get(header, 0).equals(groupVar)) {
          groupVarIndex = 1;
          xVarIndex = 0;
        }
      } else {
        s.nextLine(); // ignore header, expecting single column representing ordered xVar values
      }
      
      String[] row = s.nextLine().asString().split("\t");
      String currentXCategory = Array.get(row, xVarIndex);
      String currentGroup = null;
      if (groupVarIndex != null) {
        currentGroup = Array.get(row, groupVarIndex);
      }
      rowCount.set(1);
  
      while(s.hasNextLine()) {
        String[] row = s.nextLine().asString().split("\t");
        String xCategory = Array.get(row, xVarIndex);
        String group = null;
        boolean increment = false;
        if (groupVarIndex != null) {
          String group = Array.get(row, groupVarIndex);
          if (group.equals(currentGroup) && xCategory.equals(currentXCategory)) {
            increment = true;
          }
        } else {
          if (xCategory.equals(currentXCategory)) {
            increment = true;
          }
        }
        if (increment) {
          rowCount.set(rowCount.get() + 1);
        } else {
          JSONObject barRow = new JSONObject;
          if (currentGroup != null) {
            barRow.put("group", currentGroup)
          }
          barRow.put("label", currentXCategory); 
          barRow.put("value", rowCount);
          out.write(barRow.toString());
          currentGroup = group;
          currentXCategory = xCategory;
          rowCount.set(1);
        }
      }
      
      s.close();
      out.flush();
    } else {    
      useRConnectionWithRemoteFiles(dataStreams, connection -> {
        connection.voidEval("data <- fread(" + DATAFILE_NAME + ")");
        String[] variableNames = {"xAxisVariable",
	            						"overlayVariable",
					         			"facetVariable1",
								         "facetVariable2"};
        String[] variables = {spec.getXAxisVariable(),
    		         				spec.getOverlayVariable(),
    		  		         		Array.get(spec.getFacetVariable(),0),
    		  				         Array.get(spec.getFacetVariable(),1)};
        RList plotRefMap = new RList(new REXP(variableNames), new REXP(variables))
        connection.assign("map", plotRefMap);
        connection.voidEval("names(map) <- c('id', 'plotRef')");
        String outFile = connection.eval("bar(data, map, " + spec.getValueSpec() + ")").asString();
        RFileInputStream response = connection.openFile(outFile);
        transferStream(response, out);
        response.close();
        out.flush();
      });
    }
  }
}
