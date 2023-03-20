package org.veupathdb.service.eda.ds.plugin;

import org.gusdb.fgputil.ListBuilder;
import org.gusdb.fgputil.validation.ValidationException;
import org.veupathdb.service.eda.common.client.spec.StreamSpec;
import org.veupathdb.service.eda.common.plugin.constraint.ConstraintSpec;
import org.veupathdb.service.eda.common.plugin.constraint.DataElementSet;
import org.veupathdb.service.eda.common.plugin.util.PluginUtil;
import org.veupathdb.service.eda.ds.Resources;
import org.veupathdb.service.eda.ds.plugin.AbstractEmptyComputePlugin;
import org.veupathdb.service.eda.generated.model.APIVariableType;
import org.veupathdb.service.eda.generated.model.CategoricalCountsSpec;
import org.veupathdb.service.eda.generated.model.VariableSpec;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CategoricalCountsPlugin extends AbstractEmptyComputePlugin<CategoricalCountsPostRequest, CategoricalCountsSpec> {
  
  private static final Logger LOG = LogManager.getLogger(CategoricalCountsPlugin.class);

  private static final ManagedMap<String,CategoricalCountsPostResponse> RESULT_CACHE =
      new ManagedMap<>(5000, 2000);

  private String _cacheKey;
  private CategoricalCountsResponse _cachedResponse;

  @Override
  protected Class<CategoricalCountsSpec> getVisualizationSpecClass() {
    return CategoricalCountsSpec.class;
  }

  @Override
  public ConstraintSpec getConstraintSpec() {
    return new ConstraintSpec()
      .dependencyOrder(List.of("variable"))
      .pattern()
        .element("variable")
          .shapes(APIVariableDataShape.CATEGORICAL, APIVariableDataShape.ORDINAL)
          .description("Variable must be categorical.")
      .done();
  }
  
  @Override
  protected void validateVisualizationSpec(CategoricalCountsSpec pluginSpec) throws ValidationException {
    validateInputs(new DataElementSet()
      .var("variable", pluginSpec.getVariable()));
  }

  @Override
  protected List<StreamSpec> getRequestedStreams(CategoricalCountsSpec pluginSpec) {
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
    CategoricalCountsSpec spec = getPluginSpec();

    // this just needs to make a hashmap of values and their counts  
  }
}
