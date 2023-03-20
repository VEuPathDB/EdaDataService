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

    useRConnectionWithRemoteFiles(Resources.RSERVE_URL, dataStreams, connection -> {
      // finding medians and quantile or sd bins seems like R work
      // need to figure how to best stream the resulting json
      // ex: the below returns a json string in the format of BinRange[] for some cont var x
      // connection.voidEval("veupathUtils::toJSON(veupathUtils::getBinRanges(x, 'quantile', 10, FALSE))");
      // we could concat json strings together from multiple such calls i suppose.. plus one for formatC(median(x))
      // BUT it seems a bit like if were going to be doing work in R, we may as well do it all there and get all the JSON at once?
      // thatd let us use the streamResult method of RServeClient
    });
  }
}
