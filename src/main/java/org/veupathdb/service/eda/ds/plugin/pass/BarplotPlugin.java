package org.veupathdb.service.eda.ds.plugin.pass;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
import org.veupathdb.service.eda.ds.plugin.AbstractPlugin;
import org.veupathdb.service.eda.generated.model.APIVariableType;
import org.veupathdb.service.eda.generated.model.BarplotPostRequest;
import org.veupathdb.service.eda.generated.model.BarplotSpec;
import org.veupathdb.service.eda.generated.model.VariableSpec;

import static org.veupathdb.service.eda.ds.util.RServeClient.useRConnectionWithRemoteFiles;

public class BarplotPlugin extends AbstractPlugin<BarplotPostRequest, BarplotSpec> {

  private static final String DATAFILE_NAME = "file1.txt";

  @Override
  protected Class<BarplotSpec> getVisualizationSpecClass() {
    return BarplotSpec.class;
  }

  @Override
  protected ValidationBundle validateVisualizationSpec(BarplotSpec pluginSpec) throws ValidationException {
    ValidationBundleBuilder validation = ValidationBundle.builder(ValidationLevel.RUNNABLE);
    EntityDef entity = getValidEntity(validation, pluginSpec.getOutputEntityId());
    validateVariableNameAndType(validation, entity, "xAxisVariable", pluginSpec.getXAxisVariable(), APIVariableType.STRING);
    if (pluginSpec.getOverlayVariable() != null) {
      validateVariableNameAndType(validation, entity, "overlayVariable", pluginSpec.getOverlayVariable(), APIVariableType.STRING);
    }
    if (pluginSpec.getFacetVariable() != null) {
      for (VariableSpec facetVar : pluginSpec.getFacetVariable()) {
        validateVariableNameAndType(validation, entity, "facetVariable", facetVar, APIVariableType.STRING);
      }
    }
    return validation.build();
  }

  @Override
  protected List<StreamSpec> getRequestedStreams(BarplotSpec pluginSpec) {
    StreamSpec spec = new StreamSpec(DATAFILE_NAME, pluginSpec.getOutputEntityId());
    spec.add(pluginSpec.getXAxisVariable());
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
    BarplotSpec spec = getPluginSpec();
    EntityDef entity = getReferenceMetadata().getEntity(spec.getOutputEntityId());
    
    boolean simpleBar = true;
    // TODO consider adding facets to simpleBar ?
    if (spec.getFacetVariable() != null
         || !spec.getValueSpec().equals(BarplotSpec.ValueSpecType.COUNT)
         || dataStreams.size() != 1) {
      simpleBar = false;
    }
    
    //until i figure out the sort issue
    simpleBar = false;
    if (simpleBar) {
      int rowCount = 0;
      Scanner s = new Scanner(dataStreams.get(DATAFILE_NAME)).useDelimiter("\n");
      
      Integer groupVarIndex = null;
      int xVarIndex = 0;
      String xVar = toColNameOrEmpty(entity.getVariable(spec.getXAxisVariable()));
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
      String facetVar1 = spec.getFacetVariable() != null ? toColNameOrEmpty(spec.getFacetVariable().get(0)) : "";
      String facetVar2 = spec.getFacetVariable() != null ? toColNameOrEmpty(spec.getFacetVariable().get(1)) : "";
      String xVarEntity = spec.getXAxisVariable() != null ? spec.getXAxisVariable().getEntityId() : "";
      String overlayEntity = spec.getOverlayVariable() != null ? spec.getOverlayVariable().getEntityId() : "";
      String facetEntity1 = spec.getFacetVariable() != null ? spec.getFacetVariable().get(0).getEntityId() : "";
      String facetEntity2 = spec.getFacetVariable() != null ? spec.getFacetVariable().get(1).getEntityId() : "";
      String xVarType = spec.getXAxisVariable() != null ? entity.getVariable(spec.getXAxisVariable()).getType().toString() : "";
      String overlayType = spec.getOverlayVariable() != null ? entity.getVariable(spec.getOverlayVariable()).getType().toString() : "";
      String facetType1 = spec.getFacetVariable() != null ? entity.getVariable(spec.getFacetVariable().get(0)).getType().toString() : "";
      String facetType2 = spec.getFacetVariable() != null ? entity.getVariable(spec.getFacetVariable().get(1)).getType().toString() : "";
      
      useRConnectionWithRemoteFiles(dataStreams, connection -> {
        connection.voidEval("data <- fread('" + DATAFILE_NAME + "', na.strings=c(''))");
        String createMapString = "map <- data.frame("
            + "'plotRef'=c('xAxisVariable', "
            + "       'overlayVariable', "
            + "       'facetVariable1', "
            + "       'facetVariable2'), "
            + "'id'=c('" + xVar + "'"
            + ", '" + overlayVar + "'"
            + ", '" + facetVar1 + "'"
            + ", '" + facetVar2 + "'), "
            + "'entityId'=c('" + xVarEntity + "'"
            + ", '" + overlayEntity + "'"
            + ", '" + facetEntity1 + "'"
            + ", '" + facetEntity2 + "'), "
            + "'dataType'=c('" + xVarType + "'"
            + ", '" + overlayType + "'"
            + ", '" + facetType1 + "'"
            + ", '" + facetType2 + "'), stringsAsFactors=FALSE)";
        connection.voidEval(createMapString);
        String outFile = connection.eval("bar(data, map, '" + spec.getValueSpec().toString().toLowerCase() + "')").asString();
        try (RFileInputStream response = connection.openFile(outFile)) {
          IoUtil.transferStream(out, response);
        }
        out.flush();
      });
    }
  }
}
