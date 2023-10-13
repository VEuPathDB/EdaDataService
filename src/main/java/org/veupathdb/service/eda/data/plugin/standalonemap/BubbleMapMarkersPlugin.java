package org.veupathdb.service.eda.data.plugin.standalonemap;

import org.gusdb.fgputil.DelimitedDataParser;
import org.gusdb.fgputil.geo.GeographyUtil.GeographicPoint;
import org.gusdb.fgputil.json.JsonUtil;
import org.gusdb.fgputil.validation.ValidationException;
import org.veupathdb.service.eda.common.client.spec.StreamSpec;
import org.veupathdb.service.eda.common.plugin.constraint.ConstraintSpec;
import org.veupathdb.service.eda.common.plugin.constraint.DataElementSet;
import org.veupathdb.service.eda.data.plugin.standalonemap.aggregator.MarkerAggregator;
import org.veupathdb.service.eda.data.plugin.standalonemap.markers.MapMarkerRowProcessor;
import org.veupathdb.service.eda.data.plugin.standalonemap.markers.MarkerData;
import org.veupathdb.service.eda.data.plugin.standalonemap.markers.QuantitativeAggregateConfiguration;
import org.veupathdb.service.eda.data.core.AbstractEmptyComputePlugin;
import org.veupathdb.service.eda.data.plugin.standalonemap.markers.GeolocationViewport;
import org.veupathdb.service.eda.generated.model.APIVariableType;
import org.veupathdb.service.eda.generated.model.ColoredMapElementInfo;
import org.veupathdb.service.eda.generated.model.ColoredMapElementInfoImpl;
import org.veupathdb.service.eda.generated.model.QuantitativeOverlayConfig;
import org.veupathdb.service.eda.generated.model.StandaloneMapBubblesPostRequest;
import org.veupathdb.service.eda.generated.model.StandaloneMapBubblesPostResponse;
import org.veupathdb.service.eda.generated.model.StandaloneMapBubblesPostResponseImpl;
import org.veupathdb.service.eda.generated.model.StandaloneMapBubblesSpec;
import org.veupathdb.service.eda.generated.model.VariableSpec;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.gusdb.fgputil.FormatUtil.TAB;
import static org.veupathdb.service.eda.data.metadata.AppsMetadata.VECTORBASE_PROJECT;

public class BubbleMapMarkersPlugin extends AbstractEmptyComputePlugin<StandaloneMapBubblesPostRequest, StandaloneMapBubblesSpec> {
  private QuantitativeAggregateConfiguration _overlaySpecification = null;

  @Override
  public List<String> getProjects() {
    return List.of(VECTORBASE_PROJECT);
  }

  @Override
  public ConstraintSpec getConstraintSpec() {
    return new ConstraintSpec()
      .dependencyOrder(List.of("overlayVariable"), List.of("geoAggregateVariable", "latitudeVariable", "longitudeVariable"))
      .pattern()
        .element("geoAggregateVariable")
          .types(APIVariableType.STRING)
        .element("latitudeVariable")
          .types(APIVariableType.NUMBER)
        .element("longitudeVariable")
          .types(APIVariableType.LONGITUDE)
        .element("overlayVariable")
      .done();
  }

  @Override
  protected ClassGroup getTypeParameterClasses() {
    return new EmptyComputeClassGroup(StandaloneMapBubblesPostRequest.class, StandaloneMapBubblesSpec.class);
  }

  @Override
  protected void validateVisualizationSpec(StandaloneMapBubblesSpec pluginSpec) throws ValidationException {
    validateInputs(new DataElementSet()
      .entity(pluginSpec.getOutputEntityId())
      .var("geoAggregateVariable", pluginSpec.getGeoAggregateVariable())
      .var("latitudeVariable", pluginSpec.getLatitudeVariable())
      .var("longitudeVariable", pluginSpec.getLongitudeVariable())
      .var("overlayVariable", Optional.ofNullable(pluginSpec.getOverlayConfig())
          .map(QuantitativeOverlayConfig::getOverlayVariable)
          .orElse(null)));
    if (pluginSpec.getOverlayConfig() != null) {
      try {
        if (pluginSpec.getOverlayConfig().getAggregationConfig() == null) {
          throw new ValidationException("aggregationConfig is a required field.");
        }
        _overlaySpecification = new QuantitativeAggregateConfiguration(pluginSpec.getOverlayConfig().getAggregationConfig(),
            getUtil().getVariableDataShape(pluginSpec.getOverlayConfig().getOverlayVariable()));
      } catch (IllegalArgumentException e) {
        throw new ValidationException(e.getMessage());
      }
    }
  }

  @Override
  protected List<StreamSpec> getRequestedStreams(StandaloneMapBubblesSpec pluginSpec) {
    return List.of(
      new StreamSpec(DEFAULT_SINGLE_STREAM_NAME, pluginSpec.getOutputEntityId())
        .addVar(pluginSpec.getGeoAggregateVariable())
        .addVar(pluginSpec.getLatitudeVariable())
        .addVar(pluginSpec.getLongitudeVariable())
        .addVar(Optional.ofNullable(pluginSpec.getOverlayConfig())
            .map(QuantitativeOverlayConfig::getOverlayVariable)
            .orElse(null)));
  }

  @Override
  protected void writeResults(OutputStream out, Map<String, InputStream> dataStreams) throws IOException {
    // create scanner and line parser
    InputStreamReader isReader = new InputStreamReader(new BufferedInputStream(dataStreams.get(DEFAULT_SINGLE_STREAM_NAME)));
    BufferedReader reader = new BufferedReader(isReader);
    DelimitedDataParser parser = new DelimitedDataParser(reader.readLine(), TAB, true);

    // establish column header indexes
    StandaloneMapBubblesSpec spec = getPluginSpec();
    Function<VariableSpec, Integer> indexOf = var -> parser.indexOfColumn(getUtil().toColNameOrEmpty(var)).orElseThrow();
    int geoVarIndex  = indexOf.apply(spec.getGeoAggregateVariable());
    int latIndex     = indexOf.apply(spec.getLatitudeVariable());
    int lonIndex     = indexOf.apply(spec.getLongitudeVariable());
    Optional<QuantitativeAggregateConfiguration> overlayConfig = Optional.ofNullable(_overlaySpecification);
    Integer overlayIndex = Optional.ofNullable(_pluginSpec.getOverlayConfig())
        .map(QuantitativeOverlayConfig::getOverlayVariable)
        .map(indexOf)
        .orElse(null);

    // get map markers config
    GeolocationViewport viewport = GeolocationViewport.fromApiViewport(spec.getViewport());

    MapMarkerRowProcessor<Double> processor = new MapMarkerRowProcessor<>(geoVarIndex, latIndex, lonIndex);

    Supplier<MarkerAggregator<Double>> markerAggregatorSupplier = () -> overlayConfig
        .map(aggregateConfig -> aggregateConfig.getAverageAggregatorProvider(overlayIndex))
        .orElse(null);

    // loop through rows of data stream, aggregating stats into a map from aggregate value to stats object
    Map<String, MarkerData<Double>> aggregatedDataByGeoVal = processor.process(reader, parser, viewport, markerAggregatorSupplier);

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
      if (data.getMarkerAggregator() != null) {
        mapEle.setOverlayValue(data.getMarkerAggregator().finish());
      }
      output.add(mapEle);
    }
    StandaloneMapBubblesPostResponse response = new StandaloneMapBubblesPostResponseImpl();
    response.setMapElements(output);
    JsonUtil.Jackson.writeValue(out, response);
    out.flush();
  }
}
