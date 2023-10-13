package org.veupathdb.service.eda.data.plugin.standalonemap;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.gusdb.fgputil.DelimitedDataParser;
import org.gusdb.fgputil.geo.GeographyUtil.GeographicPoint;
import org.gusdb.fgputil.json.JsonUtil;
import org.gusdb.fgputil.validation.ValidationException;
import org.veupathdb.service.eda.common.client.spec.StreamSpec;
import org.veupathdb.service.eda.common.plugin.constraint.ConstraintSpec;
import org.veupathdb.service.eda.common.plugin.constraint.DataElementSet;
import org.veupathdb.service.eda.data.plugin.standalonemap.aggregator.MarkerAggregator;
import org.veupathdb.service.eda.data.plugin.standalonemap.aggregator.QualitativeOverlayAggregator;
import org.veupathdb.service.eda.data.plugin.standalonemap.markers.MapMarkerRowProcessor;
import org.veupathdb.service.eda.data.plugin.standalonemap.markers.MarkerData;
import org.veupathdb.service.eda.data.plugin.standalonemap.markers.OverlaySpecification;
import org.veupathdb.service.eda.data.core.AbstractEmptyComputePlugin;
import org.veupathdb.service.eda.data.plugin.standalonemap.markers.GeolocationViewport;
import org.veupathdb.service.eda.generated.model.*;

import static org.gusdb.fgputil.FormatUtil.TAB;
import static org.veupathdb.service.eda.data.metadata.AppsMetadata.VECTORBASE_PROJECT;

public class StandaloneMapMarkersPlugin extends AbstractEmptyComputePlugin<StandaloneMapMarkersPostRequest, StandaloneMapMarkersSpec> {
  private OverlaySpecification _overlaySpecification = null;

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
    return new EmptyComputeClassGroup(StandaloneMapMarkersPostRequest.class, StandaloneMapMarkersSpec.class);
  }

  @Override
  protected void validateVisualizationSpec(StandaloneMapMarkersSpec pluginSpec) throws ValidationException {
    validateInputs(new DataElementSet()
      .entity(pluginSpec.getOutputEntityId())
      .var("geoAggregateVariable", pluginSpec.getGeoAggregateVariable())
      .var("latitudeVariable", pluginSpec.getLatitudeVariable())
      .var("longitudeVariable", pluginSpec.getLongitudeVariable())
      .var("overlayVariable", Optional.ofNullable(pluginSpec.getOverlayConfig())
          .map(OverlayConfig::getOverlayVariable)
          .orElse(null)));
    if (pluginSpec.getOverlayConfig() != null) {
      try {
        _overlaySpecification = new OverlaySpecification(pluginSpec.getOverlayConfig(), getUtil()::getVariableType, getUtil()::getVariableDataShape);
      } catch (IllegalArgumentException e) {
        throw new ValidationException(e.getMessage());
      }
    }
  }

  @Override
  protected List<StreamSpec> getRequestedStreams(StandaloneMapMarkersSpec pluginSpec) {
    return List.of(
      new StreamSpec(DEFAULT_SINGLE_STREAM_NAME, pluginSpec.getOutputEntityId())
        .addVar(pluginSpec.getGeoAggregateVariable())
        .addVar(pluginSpec.getLatitudeVariable())
        .addVar(pluginSpec.getLongitudeVariable())
        .addVar(Optional.ofNullable(pluginSpec.getOverlayConfig())
            .map(OverlayConfig::getOverlayVariable)
            .orElse(null)));
  }

  @Override
  protected void writeResults(OutputStream out, Map<String, InputStream> dataStreams) throws IOException {

    // create scanner and line parser
    InputStreamReader isReader = new InputStreamReader(new BufferedInputStream(dataStreams.get(DEFAULT_SINGLE_STREAM_NAME)));
    BufferedReader reader = new BufferedReader(isReader);
    DelimitedDataParser parser = new DelimitedDataParser(reader.readLine(), TAB, true);

    // establish column header indexes
    StandaloneMapMarkersSpec spec = getPluginSpec();
    Function<VariableSpec,Integer> indexOf = var -> parser.indexOfColumn(getUtil().toColNameOrEmpty(var)).orElseThrow();
    int geoVarIndex  = indexOf.apply(spec.getGeoAggregateVariable());
    int latIndex     = indexOf.apply(spec.getLatitudeVariable());
    int lonIndex     = indexOf.apply(spec.getLongitudeVariable());
    Optional<OverlaySpecification> overlayConfig = Optional.ofNullable(_overlaySpecification);
    Integer overlayIndex = overlayConfig
        .map(OverlaySpecification::getOverlayVariable)
        .map(indexOf)
        .orElse(null);

    // get map markers config
    String valueSpec = spec.getValueSpec().getValue();
    GeolocationViewport viewport = GeolocationViewport.fromApiViewport(spec.getViewport());

    // loop through rows of data stream, aggregating stats into a map from aggregate value to stats object
    MapMarkerRowProcessor<Map<String, QualitativeOverlayAggregator.CategoricalOverlayData>> processor = new MapMarkerRowProcessor<>(geoVarIndex, latIndex, lonIndex);

    Supplier<MarkerAggregator<Map<String, QualitativeOverlayAggregator.CategoricalOverlayData>>> aggregatorSupplier = () ->
        new QualitativeOverlayAggregator(overlayConfig.map(OverlaySpecification::getOverlayRecoder).orElse(null), overlayIndex);

    Map<String, MarkerData<Map<String, QualitativeOverlayAggregator.CategoricalOverlayData>>> aggregator = processor.process(
        reader, parser, viewport, aggregatorSupplier);

    List<StandaloneMapElementInfo> output = new ArrayList<>();
    for (String key : aggregator.keySet()) {
      StandaloneMapElementInfo mapEle = new StandaloneMapElementInfoImpl();
      MarkerData<Map<String, QualitativeOverlayAggregator.CategoricalOverlayData>> data = aggregator.get(key);
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
        mapEle.setOverlayValues(convertAggregator(data.getMarkerAggregator(), valueSpec));
      }
      output.add(mapEle);
    }
    StandaloneMapMarkersPostResponse response = new StandaloneMapMarkersPostResponseImpl();
    response.setMapElements(output);
    JsonUtil.Jackson.writeValue(out, response);
    out.flush();
  }

  private List<LegacyLabeledRangeWithCountAndValue> convertAggregator(MarkerAggregator<Map<String, QualitativeOverlayAggregator.CategoricalOverlayData>> aggregator, String valueSpec) {
    return aggregator.finish().entrySet().stream()
        .map(entry -> {
          LegacyLabeledRangeWithCountAndValue bin = new LegacyLabeledRangeWithCountAndValueImpl();
          bin.setValue(valueSpec.equals(ValueSpec.PROPORTION.getValue()) ? entry.getValue().getProportion() : entry.getValue().getCount());
          bin.setBinLabel(entry.getKey());
          bin.setCount(entry.getValue().getCount());
          return bin;
        })
        .collect(Collectors.toList());
  }
}
