package org.veupathdb.service.eda.ds.plugin.standalonemap.markers;

import org.gusdb.fgputil.DelimitedDataParser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.veupathdb.service.eda.ds.plugin.standalonemap.aggregator.AveragesWithConfidence;
import org.veupathdb.service.eda.ds.plugin.standalonemap.aggregator.CollectionAveragesWithConfidenceAggregator;
import org.veupathdb.service.eda.ds.plugin.standalonemap.aggregator.MarkerAggregator;
import org.veupathdb.service.eda.generated.model.Aggregator;
import org.veupathdb.service.eda.generated.model.CategoricalAggregationConfig;
import org.veupathdb.service.eda.generated.model.CategoricalAggregationConfigImpl;
import org.veupathdb.service.eda.generated.model.ContinuousAggregationConfig;
import org.veupathdb.service.eda.generated.model.ContinuousAggregationConfigImpl;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class MapMarkerRowProcessorTest {

  @Test
  public void test() throws IOException {
    MapMarkerRowProcessor<Map<String, AveragesWithConfidence>> collectionAggregator = new MapMarkerRowProcessor<>(2, 0, 1);
    GeolocationViewport viewport = new GeolocationViewport(
        -2,
        2,
        -2,
        2
    );
    String inputData = new StringBuilder()
        .append(generateTabularRow(0.1, 0.1, "a", "1.0", "1.0"))
        .append(generateTabularRow(0.1, 0.1, "a", "90.0", "1.0"))
        .append(generateTabularRow(0.1, 0.1, "a", "500.0", "1.0"))
        .toString();
    InputStream inputStream = new ByteArrayInputStream(inputData.getBytes(StandardCharsets.UTF_8));
    Reader reader = new InputStreamReader(inputStream);
    BufferedReader bufferedReader = new BufferedReader(reader);
    final List<String> headers = List.of("E1.LAT", "E1.LON", "E1.GEO", "E1.C1", "E1.C2");
    final Map<String, Integer> headerToIndex = IntStream.range(0, headers.size())
        .boxed()
        .collect(Collectors.toMap(headers::get, i -> i));
    final DelimitedDataParser parser = new DelimitedDataParser(List.of("E1.LAT", "E1.LON", "E1.GEO", "E1.C1", "E1.C2"), ",", true);
    ContinuousAggregationConfig aggregationConfig = new ContinuousAggregationConfigImpl();
    aggregationConfig.setAggregator(Aggregator.MEAN);
    Supplier<MarkerAggregator<Map<String, AveragesWithConfidence>>> aggregator = () -> new CollectionAveragesWithConfidenceAggregator(
        headers::get,
        headerToIndex::get,
        List.of("E1.C1", "E1.C2"), // just collection vars.,
        new QuantitativeAggregateConfiguration(aggregationConfig, "continuous", "number")
    );
    Map<String, MarkerData<Map<String, AveragesWithConfidence>>> data = collectionAggregator.process(bufferedReader, parser, viewport, aggregator);
    Map<String, AveragesWithConfidence> aMarkerData = data.get("a").getMarkerAggregator().finish();

    Assertions.assertEquals(498.18, aMarkerData.get("E1.C1").getIntervalUpperBound(), 0.01);
    Assertions.assertEquals(-104.18, aMarkerData.get("E1.C1").getIntervalLowerBound(), 0.01);
    Assertions.assertEquals(0.1, data.get("a").getLatLonAvg().getCurrentAverage().getLatitude(), 0.0001);
    Assertions.assertEquals(0.1, data.get("a").getLatLonAvg().getCurrentAverage().getLongitude(), 0.0001);
  }

  @Test
  public void testProportion() throws IOException {
    MapMarkerRowProcessor<Map<String, AveragesWithConfidence>> collectionAggregator = new MapMarkerRowProcessor<>(2, 0, 1);
    GeolocationViewport viewport = new GeolocationViewport(
        -2,
        2,
        -2,
        2
    );
    String inputData = new StringBuilder()
        .append(generateTabularRow(0.1, 0.1, "a", "a", "b"))
        .append(generateTabularRow(0.1, 0.1, "a", "a", "b"))
        .append(generateTabularRow(0.1, 0.1, "a", "b", "a"))
        .toString();
    InputStream inputStream = new ByteArrayInputStream(inputData.getBytes(StandardCharsets.UTF_8));
    Reader reader = new InputStreamReader(inputStream);
    BufferedReader bufferedReader = new BufferedReader(reader);
    final List<String> headers = List.of("E1.LAT", "E1.LON", "E1.GEO", "E1.C1", "E1.C2");
    final Map<String, Integer> headerToIndex = IntStream.range(0, headers.size())
        .boxed()
        .collect(Collectors.toMap(headers::get, i -> i));
    final DelimitedDataParser parser = new DelimitedDataParser(List.of("E1.LAT", "E1.LON", "E1.GEO", "E1.C1", "E1.C2"), ",", true);
    CategoricalAggregationConfig aggregationConfig = new CategoricalAggregationConfigImpl();
    aggregationConfig.setDenominatorValues(List.of("a", "b"));
    aggregationConfig.setNumeratorValues(List.of("a"));

    Supplier<MarkerAggregator<Map<String, AveragesWithConfidence>>> aggregator = () -> new CollectionAveragesWithConfidenceAggregator(
        headers::get,
        headerToIndex::get,
        List.of("E1.C1", "E1.C2"), // just collection vars.,
        new QuantitativeAggregateConfiguration(aggregationConfig, "categorical", "string")
    );
    Map<String, MarkerData<Map<String, AveragesWithConfidence>>> data = collectionAggregator.process(bufferedReader, parser, viewport, aggregator);
    Map<String, AveragesWithConfidence> aMarkerData = data.get("a").getMarkerAggregator().finish();

    Assertions.assertEquals(1.00, aMarkerData.get("E1.C1").getIntervalUpperBound(), 0.01);
    Assertions.assertEquals(0.133, aMarkerData.get("E1.C1").getIntervalLowerBound(), 0.01);
    Assertions.assertEquals(0.666, aMarkerData.get("E1.C1").getAverage(), 0.01);
  }

  private String generateTabularRow(double lat, double lon, String geoVar, String... colectionVars) {
    return String.join(",", Double.toString(lat), Double.toString(lon), geoVar, String.join(",", colectionVars)) + "\n";
  }
}
