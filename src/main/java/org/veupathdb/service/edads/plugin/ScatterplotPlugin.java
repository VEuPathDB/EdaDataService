package org.veupathdb.service.edads.plugin;

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
import org.veupathdb.service.edads.generated.model.APIVariableType;
import org.veupathdb.service.edads.generated.model.ScatterplotPostRequest;
import org.veupathdb.service.edads.generated.model.ScatterplotSpec;
import org.veupathdb.service.edads.util.AbstractEdadsPlugin;
import org.veupathdb.service.edads.util.EntityDef;
import org.veupathdb.service.edads.util.StreamSpec;

public class ScatterplotPlugin extends AbstractEdadsPlugin<ScatterplotPostRequest, ScatterplotSpec> {

  private static final String DATAFILE_NAME = "file1.txt";

  @Override
  protected Class<ScatterplotSpec> getAnalysisSpecClass() {
    return ScatterplotSpec.class;
  }

  @Override
  protected ValidationBundle validateAnalysisSpec(ScatterplotSpec pluginSpec) throws ValidationException {
    ValidationBundleBuilder validation = ValidationBundle.builder(ValidationLevel.RUNNABLE);
    EntityDef entity = getValidEntity(validation, pluginSpec.getEntityId());
    validateVariableNameAndType(validation, entity, "xAxisVariable", pluginSpec.getXAxisVariable(), APIVariableType.NUMBER, APIVariableType.DATE);
    validateVariableNameAndType(validation, entity, "yAxisVariable", pluginSpec.getYAxisVariable(), APIVariableType.NUMBER, APIVariableType.DATE);
    if (pluginSpec.getOverlayVariable() != null) {
      validateVariableNameAndType(validation, entity, "overlayVariable", pluginSpec.getOverlayVariable(), APIVariableType.STRING);
    }
    if (pluginSpec.getFacetVariable() != null) {
      for (String facetVar : pluginSpec.getFacetVariable()) {
        validateVariableNameAndType(validation, entity, "facetVariable", facetVar, APIVariableType.STRING);
      }
    }
    return validation.build();
  }

  @Override
  protected List<StreamSpec> getRequestedStreams(ScatterplotSpec pluginSpec) {
    StreamSpec spec = new StreamSpec(DATAFILE_NAME, pluginSpec.getEntityId());
    spec.add(pluginSpec.getXAxisVariable());
    spec.add(pluginSpec.getYAxisVariable());
    if (pluginSpec.getOverlayVariable() != null) {
      spec.add(pluginSpec.getOverlayVariable());
    }
    if (pluginSpec.getFacetVariable() != null) {
      spec.addAll(pluginSpec.getFacetVariable());
    }
    return new ListBuilder<StreamSpec>(spec).toList();
  }

  @Override
  protected void writeResults(OutputStream out, Map<String, InputStream> dataStreams) throws IOException {
    ScatterplotSpec spec = getPluginSpec();
    
    boolean simpleScatter = true;
    // TODO consider adding facets to simpleBar ?
    if (spec.getFacetVariable() != null
         || !spec.getValueSpec().equals("count")
         || dataStreams.size() != 1) {
      simpleScatter = false;
    }
    
    if (simpleScatter) {
      EntityDef entity = new EntityDef(spec.getEntityId());
      Scanner s = new Scanner(dataStreams.get(0)).useDelimiter("\n");

      String xVar = entity.get(spec.getXAxisVariable()).getId();
      String yVar = entity.get(spec.getYAxisVariable()).getId();
      String groupVar = null;
      if (spec.getOverlayVariable() != null) {
        groupVar = entity.get(spec.getOverlayVariable()).getId();
      }
      String[] header = s.nextLine().split("\t");

      int xVarIndex = 0;
      int yVarIndex = 1;
      Integer groupVarIndex = null;
      for (int i = 0; i < header.length; i++) {
        if (Array.get(header, i).equals(groupVar)) {
          groupVarIndex = i;
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
        if (groupVarIndex != null) {
          String currentGroup = row[groupVarIndex];
          scatterRow.put("group", currentGroup);
        }
        scatterRow.put("seriesX", xValue); 
        scatterRow.put("seriesY", yValue);
        out.write(scatterRow.toString().getBytes());
      }
      
      s.close();
      out.flush();
    } else {
      useRConnectionWithRemoteFiles(dataStreams, connection -> {
        connection.voidEval("data <- fread('" + DATAFILE_NAME + "')");
        String overlayVar = ((spec.getOverlayVariable() == null) ? "" : spec.getOverlayVariable());
        String facetVar1 = ((spec.getFacetVariable() == null) ? "" : spec.getFacetVariable().get(0));
        String facetVar2 = ((spec.getFacetVariable() == null) ? "" : spec.getFacetVariable().get(1));
        connection.voidEval("map <- data.frame("
            + "'plotRef'=c('xAxisVariable', "
            + "       'yAxisVariable', "
            + "       'overlayVariable', "
            + "       'facetVariable1', "
            + "       'facetVariable2'), "
            + "'id'=c('" + spec.getXAxisVariable() + "'"
            + ", '" +           spec.getYAxisVariable() + "'"
            + ", '" +           overlayVar + "'"
            + ", '" +           facetVar1 + "'"
            + ", '" +           facetVar2 + "'), stringsAsFactors=FALSE)");
        String outFile = connection.eval("scattergl(data, map, '" + spec.getValueSpec().toString().toLowerCase() + "')").asString();
        RFileInputStream response = connection.openFile(outFile);
        IoUtil.transferStream(out, response);
        response.close();
        out.flush();
      }); 
    }
  }
}
