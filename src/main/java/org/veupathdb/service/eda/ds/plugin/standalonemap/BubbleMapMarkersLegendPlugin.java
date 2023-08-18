package org.veupathdb.service.eda.ds.plugin.standalonemap;

import org.gusdb.fgputil.DelimitedDataParser;
import org.gusdb.fgputil.json.JsonUtil;
import org.gusdb.fgputil.validation.ValidationException;
import org.veupathdb.service.eda.common.client.spec.StreamSpec;
import org.veupathdb.service.eda.common.plugin.constraint.ConstraintSpec;
import org.veupathdb.service.eda.common.plugin.constraint.DataElementSet;
import org.veupathdb.service.eda.ds.plugin.AbstractEmptyComputePlugin;
import org.veupathdb.service.eda.ds.plugin.AbstractPlugin;
import org.veupathdb.service.eda.ds.plugin.standalonemap.aggregator.CountAggregator;
import org.veupathdb.service.eda.ds.plugin.standalonemap.markers.QuantitativeAggregateConfiguration;
import org.veupathdb.service.eda.ds.plugin.standalonemap.aggregator.MarkerAggregator;
import org.veupathdb.service.eda.generated.model.APIVariableType;
import org.veupathdb.service.eda.generated.model.OverlayLegendConfig;
import org.veupathdb.service.eda.generated.model.SizeLegendConfig;
import org.veupathdb.service.eda.generated.model.StandaloneMapBubblesLegendPostRequest;
import org.veupathdb.service.eda.generated.model.StandaloneMapBubblesLegendPostResponse;
import org.veupathdb.service.eda.generated.model.StandaloneMapBubblesLegendPostResponseImpl;
import org.veupathdb.service.eda.generated.model.StandaloneMapBubblesLegendSpec;
import org.veupathdb.service.eda.generated.model.VariableSpec;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static org.gusdb.fgputil.FormatUtil.TAB;
import static org.veupathdb.service.eda.ds.metadata.AppsMetadata.VECTORBASE_PROJECT;

/**
 * Plugin designed to provide legend information for bubble markers. This is required for a client to interpret
 * the results of the bubble markers plugin. The bubble marker plugin provides numeric results for the size and color
 * of a bubble marker. In order to render the size or color, this plugin provides ranges representing the min/max for
 * color and size for a given configuration.
 */
public class BubbleMapMarkersLegendPlugin extends AbstractEmptyComputePlugin<StandaloneMapBubblesLegendPostRequest, StandaloneMapBubblesLegendSpec> {
  private QuantitativeAggregateConfiguration _colorSpecification = null;

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
    return new ClassGroup(StandaloneMapBubblesLegendPostRequest.class, StandaloneMapBubblesLegendSpec.class, Void.class);
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
        .var("colorVariable", Optional.ofNullable(pluginSpec.getColorLegendConfig())
            .map(config -> config.getQuantitativeOverlayConfig().getOverlayVariable())
            .orElse(null))
        .var("sizeGeoVariable", sizeConfig.map(SizeLegendConfig::getGeoAggregateVariable).orElse(null)));
    if (pluginSpec.getColorLegendConfig() != null) {
      try {
        final VariableSpec overlayVar = pluginSpec.getColorLegendConfig().getQuantitativeOverlayConfig().getOverlayVariable();
        _colorSpecification = new QuantitativeAggregateConfiguration(
            pluginSpec.getColorLegendConfig().getQuantitativeOverlayConfig().getAggregationConfig(),
            getUtil().getVariableDataShape(overlayVar),
            getUtil().getVariableType(overlayVar));
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
    Function<VariableSpec, Integer> indexOf = var -> parser.indexOfColumn(getUtil().toColNameOrEmpty(var)).orElse(null);

    Integer mostGranularGeoIndex = indexOf.apply(spec.getColorLegendConfig().getGeoAggregateVariable());
    Integer leastGranularGeoIndex = indexOf.apply(spec.getSizeConfig().getGeoAggregateVariable());
    Integer colorIndex = indexOf.apply(spec.getColorLegendConfig().getQuantitativeOverlayConfig().getOverlayVariable());

    final Map<String, MarkerAggregator<Double>> colorAggregators = new HashMap<>();
    final Map<String, MarkerAggregator<Integer>> countAggregators = new HashMap<>();
    String nextLine = reader.readLine();

    while (nextLine != null) {
      String[] row = parser.parseLineToArray(nextLine);

      // Check that color config is present and geo aggregate variable is present
      if (spec.getColorLegendConfig() != null && !(row[mostGranularGeoIndex] == null || row[mostGranularGeoIndex].isEmpty())) {
        colorAggregators.putIfAbsent(row[mostGranularGeoIndex], _colorSpecification.getAverageAggregatorProvider(colorIndex));
        colorAggregators.get(row[mostGranularGeoIndex]).addValue(row);
      }

      // Check that size config is present and geo aggregate variable is present
      if (spec.getSizeConfig() != null && !(row[leastGranularGeoIndex] == null || row[leastGranularGeoIndex].isEmpty())) {
        countAggregators.putIfAbsent(row[leastGranularGeoIndex], new CountAggregator());
        countAggregators.get(row[leastGranularGeoIndex]).addValue(null);
      }
      nextLine = reader.readLine();
    }

    // Construct response, serialize and flush output
    final StandaloneMapBubblesLegendPostResponse response = constructResponse(colorAggregators, countAggregators);
    JsonUtil.Jackson.writeValue(out, response);
    out.flush();
  }

  private StandaloneMapBubblesLegendPostResponse constructResponse(Map<String, MarkerAggregator<Double>> colorAggregators,
                                                                   Map<String, MarkerAggregator<Integer>> countAggregators) {
    final StandaloneMapBubblesLegendPostResponse response = new StandaloneMapBubblesLegendPostResponseImpl();
    final List<Double> colorValues = colorAggregators.values().stream()
        .map(MarkerAggregator::finish)
        .toList();
    final List<Integer> sizeValues = countAggregators.values().stream()
        .map(MarkerAggregator::finish)
        .toList();
    if (_pluginSpec.getColorLegendConfig() != null) {
      response.setMaxColorValue(colorValues.stream()
          .max(Comparator.comparingDouble(x -> x == null ? Double.NEGATIVE_INFINITY : x))
          .map(_colorSpecification::serializeAverage)
          .orElse(null));
      response.setMinColorValue(colorValues.stream()
          .min(Comparator.comparingDouble(x -> x == null ? Double.POSITIVE_INFINITY : x))
          .map(_colorSpecification::serializeAverage)
          .orElse(null));
    }
    if (_pluginSpec.getSizeConfig() != null) {
      response.setMinSizeValue(1);
      response.setMaxSizeValue(sizeValues.stream()
          .max(Comparator.comparingInt(Integer::intValue))
          .orElse(null));
    }
    return response;
  }
}
