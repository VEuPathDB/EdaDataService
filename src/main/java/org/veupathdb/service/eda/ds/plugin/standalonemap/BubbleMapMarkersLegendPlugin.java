package org.veupathdb.service.eda.ds.plugin.standalonemap;

import org.gusdb.fgputil.DelimitedDataParser;
import org.gusdb.fgputil.Tuples;
import org.gusdb.fgputil.geo.GeographyUtil.GeographicPoint;
import org.gusdb.fgputil.json.JsonUtil;
import org.gusdb.fgputil.validation.ValidationException;
import org.veupathdb.service.eda.common.client.spec.StreamSpec;
import org.veupathdb.service.eda.common.plugin.constraint.ConstraintSpec;
import org.veupathdb.service.eda.common.plugin.constraint.DataElementSet;
import org.veupathdb.service.eda.ds.plugin.AbstractEmptyComputePlugin;
import org.veupathdb.service.eda.ds.plugin.AbstractPlugin;
import org.veupathdb.service.eda.ds.plugin.standalonemap.markers.MapBubbleSpecification;
import org.veupathdb.service.eda.ds.plugin.standalonemap.markers.MarkerAggregator;
import org.veupathdb.service.eda.ds.plugin.standalonemap.markers.MarkerData;
import org.veupathdb.service.eda.generated.model.APIVariableType;
import org.veupathdb.service.eda.generated.model.ColorLegendConfig;
import org.veupathdb.service.eda.generated.model.ColoredMapElementInfo;
import org.veupathdb.service.eda.generated.model.ColoredMapElementInfoImpl;
import org.veupathdb.service.eda.generated.model.OverlayLegendConfig;
import org.veupathdb.service.eda.generated.model.SizeLegendConfig;
import org.veupathdb.service.eda.generated.model.StandaloneMapBubblesLegendPostRequest;
import org.veupathdb.service.eda.generated.model.StandaloneMapBubblesLegendSpec;
import org.veupathdb.service.eda.generated.model.StandaloneMapBubblesPostResponse;
import org.veupathdb.service.eda.generated.model.StandaloneMapBubblesPostResponseImpl;
import org.veupathdb.service.eda.generated.model.VariableSpec;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.gusdb.fgputil.FormatUtil.TAB;
import static org.veupathdb.service.eda.ds.metadata.AppsMetadata.VECTORBASE_PROJECT;

public class BubbleMapMarkersLegendPlugin extends AbstractEmptyComputePlugin<StandaloneMapBubblesLegendPostRequest, StandaloneMapBubblesLegendSpec> {
  private MapBubbleSpecification _colorSpecification = null;

  @Override
  public List<String> getProjects() {
    return List.of(VECTORBASE_PROJECT);
  }

  @Override
  public ConstraintSpec getConstraintSpec() {
    return new ConstraintSpec()
      .pattern()
        .element("colorGeoVariable")
          .types(APIVariableType.STRING)
        .element("sizeGeoVariable")
          .types(APIVariableType.STRING)
        .done();
  }


  @Override
  protected AbstractPlugin<StandaloneMapBubblesLegendPostRequest, StandaloneMapBubblesLegendSpec, Void>.ClassGroup getTypeParameterClasses() {
    return null;
  }

  @Override
  protected void validateVisualizationSpec(StandaloneMapBubblesLegendSpec pluginSpec) throws ValidationException {
    if (pluginSpec.getColorLegendConfig() == null && pluginSpec.getSizeConfig() == null) {
      throw new ValidationException("One of colorLegendConfig or sizeConfig must be provided.");
    }
    Optional<OverlayLegendConfig> legendConfig = Optional.ofNullable(pluginSpec.getColorLegendConfig());
    Optional<SizeLegendConfig> sizeConfig = Optional.ofNullable(pluginSpec.getSizeConfig());

    validateInputs(new DataElementSet()
        .entity(pluginSpec.getOutputEntityId())
        .var("colorGeoVariable", legendConfig.map(OverlayLegendConfig::getGeoAggregateVariable).orElse(null))
        .var("colorVariable", legendConfig.map(colorLegendConfig -> colorLegendConfig.getQuantitativeOverlayConfig().getOverlayVariable()).orElse(null))
        .var("sizeGeoVariable", sizeConfig.map(SizeLegendConfig::getGeoAggregateVariable).orElse(null)));
    if (pluginSpec.getColorLegendConfig() != null) {
      try {
        _colorSpecification = new MapBubbleSpecification(pluginSpec.getColorLegendConfig(), getUtil()::getVariableDataShape);
      } catch (IllegalArgumentException e) {
        throw new ValidationException(e.getMessage());
      }
    }
  }

  @Override
  protected List<StreamSpec> getRequestedStreams(StandaloneMapBubblesLegendSpec pluginSpec) {
    Optional<OverlayLegendConfig> legendConfig = Optional.ofNullable(pluginSpec.getColorLegendConfig());
    Optional<SizeLegendConfig> sizeConfig = Optional.ofNullable(pluginSpec.getSizeConfig());
    return List.of(
        new StreamSpec(DEFAULT_SINGLE_STREAM_NAME, pluginSpec.getOutputEntityId())
            .addVar(legendConfig.map(OverlayLegendConfig::getGeoAggregateVariable).orElse(null))
            .addVar(legendConfig.map(colorLegendConfig -> colorLegendConfig.getQuantitativeOverlayConfig().getOverlayVariable()).orElse(null))
            .addVar(sizeConfig.map(SizeLegendConfig::getGeoAggregateVariable).orElse(null)));
  }

  @Override
  protected void writeResults(OutputStream out, Map<String, InputStream> dataStreams) throws IOException {
    // create scanner and line parser
    InputStreamReader isReader = new InputStreamReader(new BufferedInputStream(dataStreams.get(DEFAULT_SINGLE_STREAM_NAME)));
    BufferedReader reader = new BufferedReader(isReader);
    DelimitedDataParser parser = new DelimitedDataParser(reader.readLine(), TAB, true);

    // establish column header indexes
    StandaloneMapBubblesLegendSpec spec = getPluginSpec();
    Function<VariableSpec, Integer> indexOf = var -> parser.indexOfColumn(getUtil().toColNameOrEmpty(var)).orElseThrow();
    Integer mostGranularGeoIndex = indexOf.apply(spec.getGeoAggregateVariable());
    Integer leastGranularGeoIndex = indexOf.apply(spec.getGeoAggregateVariable());
    Integer colorIndex = indexOf.apply(spec.getGeoAggregateVariable());

    Optional<MapBubbleSpecification> overlayConfig = Optional.ofNullable(_colorSpecification);
    Integer overlayIndex = overlayConfig
        .map(MapBubbleSpecification::getOverlayVariable)
        .map(indexOf)
        .orElse(null);

    //
    Supplier<MarkerAggregator<Double>> colorAggregatorSupplier = _colorSpecification.getAggregatorSupplier();

    final Map<String, MarkerAggregator<Double>> colorAggregators = new HashMap<>();
    final Map<String, MarkerAggregator<Tuples.TwoTuple<Integer, Integer>>> countAggregators = new HashMap<>();
    String nextLine = reader.readLine();

    while (nextLine != null) {
      String[] row = parser.parseLineToArray(nextLine);

      // entity records counts not impacted by viewport
      if (!(row[mostGranularGeoIndex] == null || row[mostGranularGeoIndex].isEmpty())) {

        MarkerAggregator<Double> colorAgg = colorAggregators.putIfAbsent(row[mostGranularGeoIndex], colorAggregatorSupplier.get());
        colorAgg.addValue(row[colorIndex]);
      }

      // entity records counts not impacted by viewport
      if (!(row[leastGranularGeoIndex] == null || row[leastGranularGeoIndex].isEmpty())) {
        MarkerAggregator<Tuples.TwoTuple<Integer, Integer>> countAgg = countAggregators.putIfAbsent(row[leastGranularGeoIndex], new CountAgg());
        countAgg.addValue(null);
      }

      nextLine = reader.readLine();
    }
    
    List<ColoredMapElementInfo> output = new ArrayList<>();
    for (String key : aggregatedDataByGeoVal.keySet()) {
      ColoredMapElementInfo mapEle = new ColoredMapElementInfoImpl();
      MarkerData<Double> data = aggregatedDataByGeoVal.get(key);
      GeographicPoint avgLatLon = data.getLatLonAvg().getCurrentAverage();
      mapEle.setGeoAggregateValue(key);
      mapEle.setEntityCount(data.getCount());
      mapEle.setAvgLat(avgLatLon.getLatitude());
      mapEle.setAvgLon(avgLatLon.getLongitude());
      mapEle.setMinLat(data.getMinLat());
      mapEle.setMaxLat(data.getMaxLat());
      mapEle.setMinLon(data.getMinLon());
      mapEle.setMaxLon(data.getMaxLon());
      mapEle.setColorValue(data.getMarkerAggregator().finish());
      output.add(mapEle);
    }
    StandaloneMapBubblesPostResponse response = new StandaloneMapBubblesPostResponseImpl();
    response.setMapElements(output);
    JsonUtil.Jackson.writeValue(out, response);
    out.flush();
  }
}
