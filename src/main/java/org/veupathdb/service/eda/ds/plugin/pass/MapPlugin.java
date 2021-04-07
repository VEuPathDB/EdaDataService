package org.veupathdb.service.eda.ds.plugin.pass;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import org.gusdb.fgputil.ListBuilder;
import org.gusdb.fgputil.validation.ValidationBundle;
import org.gusdb.fgputil.validation.ValidationBundle.ValidationBundleBuilder;
import org.gusdb.fgputil.validation.ValidationException;
import org.gusdb.fgputil.validation.ValidationLevel;
import org.veupathdb.service.eda.common.model.EntityDef;
import org.veupathdb.service.eda.common.client.spec.StreamSpec;
import org.veupathdb.service.eda.common.model.ReferenceMetadata;
import org.veupathdb.service.eda.ds.constraints.ConstraintSpec;
import org.veupathdb.service.eda.ds.constraints.DataElementSet;
import org.veupathdb.service.eda.ds.plugin.AbstractPlugin;
import org.veupathdb.service.eda.generated.model.APIVariableDataShape;
import org.veupathdb.service.eda.generated.model.APIVariableType;
import org.veupathdb.service.eda.generated.model.MapPostRequest;
import org.veupathdb.service.eda.generated.model.MapSpec;
import org.json.JSONObject;

public class MapPlugin extends AbstractPlugin<MapPostRequest, MapSpec> {

  @Override
  public String getDisplayName() {
    return "Record Count";
  }

  @Override
  public String getDescription() {
    return "Counts how many rows in a single stream of records";
  }

  @Override
  protected Class<MapSpec> getVisualizationSpecClass() {
    return MapSpec.class;
  }

  @Override
  public ConstraintSpec getConstraintSpec() {
    return new ConstraintSpec()
      .pattern()
        .element("geoAggregateVariable")
          .types(APIVariableType.STRING)
        .element("latitudeVariable")
          .types(APIVariableType.NUMBER)
        .element("longitudeVariable")
          .types(APIVariableType.LONGITUDE)
      .done();
  }

  @Override
  protected void validateVisualizationSpec(MapSpec pluginSpec) throws ValidationException {
    validateInputs(new DataElementSet()
      .entity(pluginSpec.getOutputEntityId())
      .var("geoAggregateVariable", pluginSpec.getGeoAggregateVariable())
      .var("latitudeVariable", pluginSpec.getLatitudeVariable())
      .var("longitudeVariable", pluginSpec.getLongitudeVariable()));
  }

  @Override
  protected List<StreamSpec> getRequestedStreams(MapSpec pluginSpec) {
    return ListBuilder.asList(
      new StreamSpec(DEFAULT_SINGLE_STREAM_NAME, pluginSpec.getOutputEntityId())
        .addVar(pluginSpec.getGeoAggregateVariable())
        .addVar(pluginSpec.getLatitudeVariable())
        .addVar(pluginSpec.getLongitudeVariable()));
  }

  @Override
  protected void writeResults(OutputStream out, Map<String, InputStream> dataStreams) throws IOException {
    MapSpec spec = getPluginSpec();
    
    Map<String, List<Double>> geoVarLatMap = new HashMap<String, List<Double>>();
    Map<String, List<Double>> geoVarLonMap = new HashMap<String, List<Double>>();
    Map<String, Integer> geoVarEntityCount = new HashMap<String, Integer>();
    Scanner s = new Scanner(dataStreams.get(DEFAULT_SINGLE_STREAM_NAME)).useDelimiter("\n");

    EntityDef entity = getReferenceMetadata().getEntity(spec.getOutputEntityId());
    String entityIdCol = entity.getIdColumnName();
    String geoAggregateVar = toColNameOrEmpty(spec.getGeoAggregateVariable());
    String lonVar = toColNameOrEmpty(spec.getLongitudeVariable());
    String latVar = toColNameOrEmpty(spec.getLatitudeVariable());
    String[] header = s.nextLine().split("\t");
    
    int idIndex = 0;
    int geoVarIndex = 1;
    int latIndex = 2;
    int lonIndex = 3;
    for (int i = 0; i < header.length; i++) {
      if (header[i].equals(entityIdCol)) {
        idIndex = i;
      } else if (header[i].equals(geoAggregateVar)) {
        geoVarIndex = i;
      } else if (header[i].equals(latVar)) {
        latIndex = i;
      } else if (header[i].equals(lonVar)) {
        lonIndex = i;
      }
    }

    // FIXME: lat/lon cannot be simply averaged, right?  Don't we need to take into account N/S, E/W?
    while(s.hasNextLine()) {
      String[] row = s.nextLine().split("\t");
      geoVarLatMap.putIfAbsent(row[geoVarIndex], new ArrayList<Double>());
      geoVarLatMap.get(row[geoVarIndex]).add(Double.valueOf(row[latIndex]));
      geoVarLonMap.putIfAbsent(row[geoVarIndex], new ArrayList<Double>());
      geoVarLonMap.get(row[geoVarIndex]).add(Double.valueOf(row[lonIndex]));
      geoVarEntityCount.putIfAbsent(row[geoVarIndex], 0);
      geoVarEntityCount.put(row[geoVarIndex], geoVarEntityCount.get(row[geoVarIndex])+1);
    }

    for (Map.Entry mapElement : geoVarEntityCount.entrySet()) { 
      String key = (String)mapElement.getKey();
      Double latMin = Collections.min(geoVarLatMap.get(key));
      Double latMax = Collections.max(geoVarLatMap.get(key));
      Double latAvg = findAverage(geoVarLatMap.get(key));
      Double lonMin = Collections.min(geoVarLonMap.get(key));
      Double lonMax = Collections.max(geoVarLonMap.get(key));
      Double lonAvg = findAverage(geoVarLonMap.get(key));
      int entityCount = ((int)mapElement.getValue()); 
      out.write(new JSONObject().put("geoAggregateValue", key)
                                .put("entityCount", entityCount)
                                .put("avgLat", latAvg)
                                .put("avgLon", lonAvg)
                                .put("maxLat", latMax)
                                .put("maxLon", lonMax)
                                .put("minLat", latMin)
                                .put("minLon", lonMin)
                                .toString().getBytes()); 
    }  
    out.flush();
  }
  
  private double findAverage(List <Double> values) {
    Double sum = 0.0;
    if(!values.isEmpty()) {
      for (Double value : values) {
          sum += value;
      }
      return sum / values.size();
    }
    return sum;
  }
}
