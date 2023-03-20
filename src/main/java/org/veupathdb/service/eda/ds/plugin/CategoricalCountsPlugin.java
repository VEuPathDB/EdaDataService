package org.veupathdb.service.eda.ds.plugin;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gusdb.fgputil.cache.ManagedMap;
import org.gusdb.fgputil.DelimitedDataParser;
import org.gusdb.fgputil.EncryptionUtil;
import org.gusdb.fgputil.json.JsonUtil;
import org.gusdb.fgputil.ListBuilder;
import org.gusdb.fgputil.validation.ValidationException;
import org.json.JSONObject;
import org.veupathdb.service.eda.common.client.spec.StreamSpec;
import org.veupathdb.service.eda.common.plugin.constraint.ConstraintSpec;
import org.veupathdb.service.eda.common.plugin.constraint.DataElementSet;
import org.veupathdb.service.eda.common.plugin.util.PluginUtil;
import org.veupathdb.service.eda.ds.Resources;
import org.veupathdb.service.eda.ds.plugin.AbstractEmptyComputePlugin;
import org.veupathdb.service.eda.generated.model.APIVariableType;
import org.veupathdb.service.eda.generated.model.APIVariableDataShape;
import org.veupathdb.service.eda.generated.model.CategoricalCountsSpec;
import org.veupathdb.service.eda.generated.model.CategoricalCountsPostRequest;
import org.veupathdb.service.eda.generated.model.CategoricalCountsPostResponse;
import org.veupathdb.service.eda.generated.model.VariableSpec;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.gusdb.fgputil.FormatUtil.TAB;

public class CategoricalCountsPlugin extends AbstractEmptyComputePlugin<CategoricalCountsPostRequest, CategoricalCountsSpec> {
  
  private static final Logger LOG = LogManager.getLogger(CategoricalCountsPlugin.class);

  private static final ManagedMap<String,CategoricalCountsPostResponse> RESULT_CACHE =
      new ManagedMap<>(5000, 2000);

  private String _cacheKey;
  private CategoricalCountsPostResponse _cachedResponse;

  @Override
  protected Class<CategoricalCountsPostRequest> getVisualizationRequestClass() {
    return CategoricalCountsPostRequest.class;
  }

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
    String entityId = pluginSpec.getVariable().getEntityId();
    _cacheKey = EncryptionUtil.md5(
        JsonUtil.serializeObject(getSubsetFilters()) +
        "|" + JsonUtil.serializeObject(pluginSpec)
    );
    _cachedResponse = RESULT_CACHE.get(_cacheKey);
    if (_cachedResponse != null) LOG.info("Found cached result.");

    return _cachedResponse != null ? Collections.emptyList() : ListBuilder.asList(
      new StreamSpec(DEFAULT_SINGLE_STREAM_NAME, pluginSpec.getVariable().getEntityId())
        .addVar(pluginSpec.getVariable()));
  }

  @Override
  protected void writeResults(OutputStream out, Map<String, InputStream> dataStreams) throws IOException {
    CategoricalCountsSpec spec = getPluginSpec();

    // create scanner and line parser
    InputStreamReader isReader = new InputStreamReader(new BufferedInputStream(dataStreams.get(DEFAULT_SINGLE_STREAM_NAME)));
    BufferedReader reader = new BufferedReader(isReader);
    DelimitedDataParser parser = new DelimitedDataParser(reader.readLine(), TAB, true);

    // establish column header indexes
    Function<VariableSpec,Integer> indexOf = var -> parser.indexOfColumn(getUtil().toColNameOrEmpty(var)).orElseThrow();
    int varIndex = indexOf.apply(spec.getVariable());

    // this just needs to make a hashmap of values and their counts
    Map<String, MutableInt> categoryCounts = new HashMap<>();
    String nextLine = reader.readLine();

    while (nextLine != null) {
      String[] row = parser.parseLineToArray(nextLine);

      categoryCounts.putIfAbsent(row[varIndex], new MutableInt());
      categoryCounts.get(row[varIndex]).increment();

      nextLine = reader.readLine();
    }

    // begin output object and single property containing array of elements
    out.write("{\"categories\":[".getBytes(StandardCharsets.UTF_8));
    boolean first = true;
    for (String key : categoryCounts.keySet()) {
      // write commas between elements
      if (first) first = false; else out.write(',');
      out.write(new JSONObject()
        .put("binLabel", key)
        .put("value", categoryCounts.get(key))
        .toString()
        .getBytes(StandardCharsets.UTF_8)
      );
    }
    // close
    out.write("]}".getBytes());
    out.flush();

  }
 
  // used this somewhere else recently too.. is there a better place for this to live? or some util somewhere already exists?
  private static class MutableInt {
    int value = 0;
    public void increment() { ++value;      }
    public void set(int x)  { value = x;    }
    public int  get()       { return value; }
  }
}
