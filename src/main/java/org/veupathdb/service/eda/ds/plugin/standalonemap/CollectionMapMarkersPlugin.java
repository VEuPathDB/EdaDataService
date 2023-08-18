package org.veupathdb.service.eda.ds.plugin.standalonemap;

import org.gusdb.fgputil.DelimitedDataParser;
import org.gusdb.fgputil.json.JsonUtil;
import org.gusdb.fgputil.validation.ValidationException;
import org.veupathdb.service.eda.common.client.spec.StreamSpec;
import org.veupathdb.service.eda.common.plugin.constraint.ConstraintSpec;
import org.veupathdb.service.eda.ds.plugin.AbstractEmptyComputePlugin;
import org.veupathdb.service.eda.ds.plugin.AbstractPlugin;
import org.veupathdb.service.eda.ds.plugin.standalonemap.aggregator.AveragesWithConfidence;
import org.veupathdb.service.eda.ds.plugin.standalonemap.aggregator.CollectionAveragesWithConfidenceAggregator;
import org.veupathdb.service.eda.ds.plugin.standalonemap.markers.GeolocationViewport;
import org.veupathdb.service.eda.ds.plugin.standalonemap.markers.MapMarkerRowProcessor;
import org.veupathdb.service.eda.ds.plugin.standalonemap.aggregator.MarkerAggregator;
import org.veupathdb.service.eda.ds.plugin.standalonemap.markers.MarkerData;
import org.veupathdb.service.eda.ds.plugin.standalonemap.markers.QuantitativeAggregateConfiguration;
import org.veupathdb.service.eda.ds.utils.ValidationUtils;
import org.veupathdb.service.eda.generated.model.APIVariableType;
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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
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
    return new ConstraintSpec()
        .dependencyOrder(List.of("geoAggregateVariable", "latitudeVariable", "longitudeVariable"))
        .pattern()
          .element("geoAggregateVariable")
            .types(APIVariableType.STRING)
          .element("latitudeVariable")
            .types(APIVariableType.NUMBER)
          .element("longitudeVariable")
            .types(APIVariableType.NUMBER)
        .done();
  }


  @Override
  protected AbstractPlugin<StandaloneCollectionMapMarkerPostRequest, StandaloneCollectionMapMarkerSpec, Void>.ClassGroup getTypeParameterClasses() {
    return new ClassGroup(StandaloneCollectionMapMarkerPostRequest.class, StandaloneCollectionMapMarkerSpec.class, Void.class);
  }

  @Override
  protected void validateVisualizationSpec(StandaloneCollectionMapMarkerSpec pluginSpec) throws ValidationException {
    if (pluginSpec.getCollection() == null) {
      throw new ValidationException("Collection information must be specified.");
    }
    ValidationUtils.validateCollectionMembers(getUtil(),
        pluginSpec.getCollection().getCollection(),
        pluginSpec.getCollection().getSelectedMembers());
    if (pluginSpec.getAggregatorConfig() != null) {
      try {
        _aggregateConfig = new QuantitativeAggregateConfiguration(pluginSpec.getAggregatorConfig(),
            getUtil().getCollectionDataShape(pluginSpec.getCollection().getCollection()),
            getUtil().getCollectionType(pluginSpec.getCollection().getCollection()));
      } catch (IllegalArgumentException e) {
        throw new ValidationException(e.getMessage());
      }
    }
  }

  @Override
  protected List<StreamSpec> getRequestedStreams(StandaloneCollectionMapMarkerSpec pluginSpec) {
    StreamSpec streamSpec = new StreamSpec(DEFAULT_SINGLE_STREAM_NAME, pluginSpec.getOutputEntityId());
    streamSpec.addVars(pluginSpec.getCollection().getSelectedMembers());
    return List.of(streamSpec);
  }

  @Override
  protected void writeResults(OutputStream out, Map<String, InputStream> dataStreams) throws IOException {
    InputStreamReader isReader = new InputStreamReader(new BufferedInputStream(dataStreams.get(DEFAULT_SINGLE_STREAM_NAME)));
    BufferedReader reader = new BufferedReader(isReader);
    DelimitedDataParser parser = new DelimitedDataParser(reader.readLine(), TAB, true);

    StandaloneCollectionMapMarkerSpec spec = getPluginSpec();
    Function<String, Integer> indexOf = var -> parser.indexOfColumn(var).orElseThrow();
    Function<Integer, String> indexToVarId = index -> parser.getColumnNames().get(index);
    List<String> memberVarColNames = spec.getCollection().getSelectedMembers().stream()
        .map(getUtil()::toColNameOrEmpty)
        .toList();

    // For each marker, aggregate all data into a Map of collection member ID to stats containing averages and confidence intervals
    final Supplier<MarkerAggregator<Map<String, AveragesWithConfidence>>> aggSupplier = () -> new CollectionAveragesWithConfidenceAggregator(indexToVarId,
        indexOf, memberVarColNames, _aggregateConfig);

    // Establish column header indexes
    int geoVarIndex = indexOf.apply(getUtil().toColNameOrEmpty(spec.getGeoAggregateVariable()));
    int latIndex = indexOf.apply(getUtil().toColNameOrEmpty(spec.getLatitudeVariable()));
    int lonIndex = indexOf.apply(getUtil().toColNameOrEmpty(spec.getLongitudeVariable()));

    GeolocationViewport viewport = GeolocationViewport.fromApiViewport(spec.getViewport());
    MapMarkerRowProcessor<Map<String, AveragesWithConfidence>> processor = new MapMarkerRowProcessor<>(geoVarIndex, latIndex, lonIndex);
    Map<String, MarkerData<Map<String, AveragesWithConfidence>>> markerDataById = processor.process(reader, parser, viewport, aggSupplier);

    // Construct response, serialize and flush output
    final StandaloneCollectionMapMarkerPostResponse response = new StandaloneCollectionMapMarkerPostResponseImpl();
    response.setMarkers(markerDataById.entrySet().stream().map(entry -> {
      final CollectionMapMarkerElement ele = new CollectionMapMarkerElementImpl();
      ele.setAvgLat(entry.getValue().getLatLonAvg().getCurrentAverage().getLatitude());
      ele.setAvgLon(entry.getValue().getLatLonAvg().getCurrentAverage().getLongitude());
      ele.setMaxLat(entry.getValue().getMaxLat());
      ele.setMaxLon(entry.getValue().getMaxLon());
      ele.setEntityCount(ele.getEntityCount());
      ele.setValues(entry.getValue().getMarkerAggregator().finish().values().stream()
          .map(markerAggregate -> translateToOutput(markerAggregate, entry.getKey()))
          .collect(Collectors.toList()));
      return ele;
    }).collect(Collectors.toList()));

    JsonUtil.Jackson.writeValue(out, response);
    out.flush();
  }

  private CollectionMemberAggregate translateToOutput(AveragesWithConfidence averagesWithConfidence, String variableId) {
    final CollectionMemberAggregate collectionMemberResult = new CollectionMemberAggregateImpl();
    collectionMemberResult.setValue(averagesWithConfidence.getAverage());
    collectionMemberResult.setN(averagesWithConfidence.getN());
    collectionMemberResult.setVariableId(variableId);
    final NumberRange range = new NumberRangeImpl();
    range.setMin(averagesWithConfidence.getIntervalLowerBound());
    range.setMax(averagesWithConfidence.getIntervalUpperBound());
    collectionMemberResult.setConfidenceInterval(range);
    return collectionMemberResult;
  }
}
