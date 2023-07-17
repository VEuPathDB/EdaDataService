package org.veupathdb.service.eda.ds.plugin.standalonemap;

import com.google.common.collect.Sets;
import org.gusdb.fgputil.DelimitedDataParser;
import org.gusdb.fgputil.json.JsonUtil;
import org.gusdb.fgputil.validation.ValidationException;
import org.veupathdb.service.eda.common.client.spec.StreamSpec;
import org.veupathdb.service.eda.common.model.VariableDef;
import org.veupathdb.service.eda.common.plugin.constraint.ConstraintSpec;
import org.veupathdb.service.eda.ds.plugin.AbstractEmptyComputePlugin;
import org.veupathdb.service.eda.ds.plugin.AbstractPlugin;
import org.veupathdb.service.eda.ds.plugin.standalonemap.markers.AveragesWithConfidence;
import org.veupathdb.service.eda.ds.plugin.standalonemap.markers.CollectionAveragesWithConfidenceAggregator;
import org.veupathdb.service.eda.ds.plugin.standalonemap.markers.GeolocationViewport;
import org.veupathdb.service.eda.ds.plugin.standalonemap.markers.MapMarkerRowProcessor;
import org.veupathdb.service.eda.ds.plugin.standalonemap.markers.MarkerAggregator;
import org.veupathdb.service.eda.ds.plugin.standalonemap.markers.MarkerData;
import org.veupathdb.service.eda.ds.plugin.standalonemap.markers.QuantitativeAggregateConfiguration;
import org.veupathdb.service.eda.generated.model.CollectionMapMarkerElement;
import org.veupathdb.service.eda.generated.model.CollectionMapMarkerElementImpl;
import org.veupathdb.service.eda.generated.model.CollectionMemberAggregate;
import org.veupathdb.service.eda.generated.model.CollectionMemberAggregateImpl;
import org.veupathdb.service.eda.generated.model.NumberRange;
import org.veupathdb.service.eda.generated.model.NumberRangeImpl;
import org.veupathdb.service.eda.generated.model.StandaloneCollectionMapMarkerPostRequest;
import org.veupathdb.service.eda.generated.model.StandaloneCollectionMapMarkerPostResponse;
import org.veupathdb.service.eda.generated.model.StandaloneCollectionMapMarkerPostResponseImpl;
import org.veupathdb.service.eda.generated.model.StandaloneCollectionMapMarkerSpec;
import org.veupathdb.service.eda.generated.model.VariableSpec;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.gusdb.fgputil.FormatUtil.TAB;
import static org.veupathdb.service.eda.ds.metadata.AppsMetadata.VECTORBASE_PROJECT;

public class CollectionMapMarkersPlugin extends AbstractEmptyComputePlugin<StandaloneCollectionMapMarkerPostRequest, StandaloneCollectionMapMarkerSpec> {

  private QuantitativeAggregateConfiguration _aggregateConfig;

  @Override
  public List<String> getProjects() {
    return List.of(VECTORBASE_PROJECT);
  }

  @Override
  public ConstraintSpec getConstraintSpec() {
    // TODO stub implementation
    return new ConstraintSpec();
  }


  @Override
  protected AbstractPlugin<StandaloneCollectionMapMarkerPostRequest, StandaloneCollectionMapMarkerSpec, Void>.ClassGroup getTypeParameterClasses() {
    return new ClassGroup(StandaloneCollectionMapMarkerPostRequest.class, StandaloneCollectionMapMarkerSpec.class, Void.class);
  }

  @Override
  protected void validateVisualizationSpec(StandaloneCollectionMapMarkerSpec pluginSpec) throws ValidationException {
    final List<VariableDef> allMembers = getUtil().getCollectionMembers(pluginSpec.getCollection());
    final Set<String> selectedMemberIds = pluginSpec.getSelectedMemberVariables().stream()
        .map(VariableSpec::getVariableId)
        .collect(Collectors.toSet());
    final Set<String> allMemberIds = allMembers.stream()
        .map(VariableDef::getVariableId)
        .collect(Collectors.toSet());
    final Set<String> invalidMemberIds = Sets.difference(selectedMemberIds, allMemberIds);
    if (!invalidMemberIds.isEmpty()) {
      throw new ValidationException("Specified member variables must belong to the specified collection. " +
          "The following were not found in collection: " + invalidMemberIds);
    }
    if (pluginSpec.getAggregationConfig() != null) {
      try {
        _aggregateConfig = new QuantitativeAggregateConfiguration(pluginSpec.getAggregationConfig(), getUtil()::getVariableDataShape);
      } catch (IllegalArgumentException e) {
        throw new ValidationException(e.getMessage());
      }
    }
  }

  @Override
  protected List<StreamSpec> getRequestedStreams(StandaloneCollectionMapMarkerSpec pluginSpec) {
    StreamSpec streamSpec = new StreamSpec(DEFAULT_SINGLE_STREAM_NAME, pluginSpec.getOutputEntityId());
    pluginSpec.getSelectedMemberVariables().forEach(streamSpec::addVar);
    return List.of(streamSpec);
  }

  @Override
  protected void writeResults(OutputStream out, Map<String, InputStream> dataStreams) throws IOException {
    InputStreamReader isReader = new InputStreamReader(new BufferedInputStream(dataStreams.get(DEFAULT_SINGLE_STREAM_NAME)));
    BufferedReader reader = new BufferedReader(isReader);
    DelimitedDataParser parser = new DelimitedDataParser(reader.readLine(), TAB, true);

    // establish column header indexes
    StandaloneCollectionMapMarkerSpec spec = getPluginSpec();
    Function<String, Integer> indexOf = var -> parser.indexOfColumn(var).orElseThrow();
    Function<Integer, String> indexToVarId = index -> parser.getColumnNames().get(index);
    List<String> varColNames = spec.getSelectedMemberVariables().stream()
        .map(getUtil()::toColNameOrEmpty)
        .toList();

    final Supplier<MarkerAggregator<Map<String, AveragesWithConfidence>>> agg = () -> new CollectionAveragesWithConfidenceAggregator(indexToVarId,
        indexOf, varColNames, _aggregateConfig.getVariableValueQuantifier());
    int geoVarIndex  = indexOf.apply(getUtil().toColNameOrEmpty(spec.getGeoAggregateVariable()));
    int latIndex     = indexOf.apply(getUtil().toColNameOrEmpty(spec.getLatitudeVariable()));
    int lonIndex     = indexOf.apply(getUtil().toColNameOrEmpty(spec.getLongitudeVariable()));

    GeolocationViewport viewport = GeolocationViewport.fromApiViewport(spec.getViewport());

    MapMarkerRowProcessor<Map<String, AveragesWithConfidence>> processor = new MapMarkerRowProcessor<>(geoVarIndex, latIndex, lonIndex);
    Map<String, MarkerData<Map<String, AveragesWithConfidence>>> markerDataById = processor.process(reader, parser, viewport, agg);

    // Construct response, serialize and flush output
    final StandaloneCollectionMapMarkerPostResponse response = new StandaloneCollectionMapMarkerPostResponseImpl();
    response.setMarkers(markerDataById.values().stream().map(mapMarkerData -> {
      final CollectionMapMarkerElement ele = new CollectionMapMarkerElementImpl();
      ele.setAvgLat(mapMarkerData.getLatLonAvg().getCurrentAverage().getLatitude());
      ele.setAvgLon(mapMarkerData.getLatLonAvg().getCurrentAverage().getLongitude());
      ele.setMaxLat(mapMarkerData.getMaxLat());
      ele.setMaxLon(mapMarkerData.getMaxLon());
      ele.setEntityCount(ele.getEntityCount());
      ele.setValues(mapMarkerData.getMarkerAggregator().finish().values().stream()
          .map(averagesWithConfidence -> {
            final CollectionMemberAggregate collectionMemberResult = new CollectionMemberAggregateImpl();
            collectionMemberResult.setMean(averagesWithConfidence.getMean());
            collectionMemberResult.setMedian(averagesWithConfidence.getMedian());
            final NumberRange range = new NumberRangeImpl();
            range.setMin(averagesWithConfidence.getIntervalLowerBound());
            range.setMax(averagesWithConfidence.getIntervalUpperBound());
            collectionMemberResult.setConfidenceInterval(range);
            collectionMemberResult.setN(averagesWithConfidence.getN());
            return collectionMemberResult;
          }).collect(Collectors.toList()));
      return ele;
    }).collect(Collectors.toList()));
    JsonUtil.Jackson.writeValue(out, response);
    out.flush();
  }
}
