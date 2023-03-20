package org.veupathdb.service.eda.ds.plugin;

import org.gusdb.fgputil.ListBuilder;
import org.gusdb.fgputil.validation.ValidationException;
import org.veupathdb.service.eda.common.client.spec.StreamSpec;
import org.veupathdb.service.eda.common.plugin.constraint.ConstraintSpec;
import org.veupathdb.service.eda.common.plugin.constraint.DataElementSet;
import org.veupathdb.service.eda.common.plugin.util.PluginUtil;
import org.veupathdb.service.eda.common.plugin.util.RFileSetProcessor;
import org.veupathdb.service.eda.ds.Resources;
import org.veupathdb.service.eda.ds.plugin.AbstractEmptyComputePlugin;
import org.veupathdb.service.eda.generated.model.APIVariableType;
import org.veupathdb.service.eda.generated.model.ContinuousVariableMetadataSpec;
import org.veupathdb.service.eda.generated.model.VariableSpec;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.veupathdb.service.eda.common.plugin.util.RServeClient.streamResult;
import static org.veupathdb.service.eda.common.plugin.util.RServeClient.useRConnectionWithProcessedRemoteFiles;

public class FilteredVariableMetadataPlugin extends AbstractEmptyComputePlugin<ContinuousVariableMetadataPostRequest, ContinuousVariableMetadataSpec> {
  
  private static final Logger LOG = LogManager.getLogger(FilteredVariableMetadataPlugin.class);

  private static final ManagedMap<String,ContinuousVariableMetadataPostResponse> RESULT_CACHE =
      new ManagedMap<>(5000, 2000);

  private String _cacheKey;
  private ContinuousVariableMetadataResponse _cachedResponse;

  @Override
  protected Class<ContinuousVariableMetadataSpec> getVisualizationSpecClass() {
    return ContinuousVariableMetadataSpec.class;
  }

  @Override
  public ConstraintSpec getConstraintSpec() {
    return new ConstraintSpec()
      .dependencyOrder(List.of("variable"))
      .pattern()
        .element("variable")
          .shapes(APIVariableDataShape.CONTINUOUS)
          .description("Variable must be continuous.")
      .done();
  }
  
  @Override
  protected void validateVisualizationSpec(ContinuousVariableMetadataSpec pluginSpec) throws ValidationException {
    validateInputs(new DataElementSet()
      .var("variable", pluginSpec.getVariable()));
  }

  @Override
  protected List<StreamSpec> getRequestedStreams(ContinuousVariableMetadataSpec pluginSpec) {
    String entityId = pluginSpec.getEntityId();
    _cacheKey = EncryptionUtil.md5(
        JsonUtil.serializeObject(getSubsetFilters()) +
        "|" + JsonUtil.serializeObject(pluginSpec)
    );
    _cachedResponse = RESULT_CACHE.get(_cacheKey);
    if (_cachedResponse != null) LOG.info("Found cached result.");

    return _cachedResponse != null ? Collections.emptyList() : ListBuilder.asList(
      new StreamSpec(DEFAULT_SINGLE_STREAM_NAME, pluginSpec.getVariable().getEntity())
        .addVar(pluginSpec.getVariable()));
  }

  @Override
  protected void writeResults(OutputStream out, Map<String, InputStream> dataStreams) throws IOException {
    PluginUtil util = getUtil();
    ContinuousVariableMetadataSpec spec = getPluginSpec();
    var jsonWrapper = new Object(){ String value = "{"; };
    // TODO i dont actually know if this returns strings like 'median'. never done this w raml before...
    List<String> requestedMetadata = spec.getMetadata();
    boolean first = true;

    useRConnectionWithRemoteFiles(Resources.RSERVE_URL, dataStreams, connection -> {
      connection.voidEval(util.getVoidEvalFreadCommand(DEFAULT_SINGLE_STREAM_NAME, spec.getVariable()));
      // for convenience
      connection.voidEval("x <- " + DEFAULT_SINGLE_STREAM_NAME + "$" + util.toColNameOrEmpty(spec.getVariable()));

      if (requestedMetadata.contains("binRanges")) {
        // TODO add support for user-defined N bins? for now 10..
        String equalIntervalJson = connection.eval("veupathUtils::toJSON(veupathUtils::getBinRanges(x, 'equalInterval', 10, FALSE))");
        String quantileJson = connection.eval("veupathUtils::toJSON(veuapthUtils::getBinRanges(x, 'quantile', 10, FALSE))");
        // sd bins return 6 bins at most, no user control supported in R currently
        String sdJson = connection.eval("veupathUtils::toJSON(veupathUtils::getBinRanges(x, 'sd', NULL, FALSE))");
        jsonWrapper.value += "\"binRanges\":{\"equalInterval\":" + equalIntervalJson + 
                                           ",\"quantile\":" + quantileJson + 
                                           ",\"standardDeviation\":" + sdJson + "}";
        first = false;
      }

      if (requestedMetadata.contains("median")) {
        String medianJson = connection.eval("jsonlite::toJSON(jsonlite::unbox(formatC(median(x))))");
        medianJson += first ? "\"median\":{" : ",\"median\":{";
        jsonWrapper.value += medianJson + "}";
        first = false;
      }

      jsonWrapper.value += "}";
    });

    out.write(jsonWrapper.value.getBytes(StandardCharsets.UTF_8));
    out.flush();
  }
}
