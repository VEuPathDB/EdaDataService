package org.veupathdb.service.eda.ds.plugin.filteredmetadata;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gusdb.fgputil.EncryptionUtil;
import org.gusdb.fgputil.cache.ManagedMap;
import org.gusdb.fgputil.cache.ValueProductionException;
import org.gusdb.fgputil.json.JsonUtil;
import org.gusdb.fgputil.validation.ValidationException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.veupathdb.service.eda.common.client.spec.StreamSpec;
import org.veupathdb.service.eda.common.plugin.constraint.ConstraintSpec;
import org.veupathdb.service.eda.common.plugin.constraint.DataElementSet;
import org.veupathdb.service.eda.common.plugin.util.PluginUtil;
import org.veupathdb.service.eda.ds.Resources;
import org.veupathdb.service.eda.ds.core.AbstractEmptyComputePlugin;
import org.veupathdb.service.eda.generated.model.APIVariableDataShape;
import org.veupathdb.service.eda.generated.model.APIVariableType;
import org.veupathdb.service.eda.generated.model.ContinuousVariableMetadataPostRequest;
import org.veupathdb.service.eda.generated.model.ContinuousVariableMetadataSpec;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.veupathdb.service.eda.common.plugin.util.RServeClient.useRConnectionWithRemoteFiles;

public class ContinuousVariablePlugin extends AbstractEmptyComputePlugin<ContinuousVariableMetadataPostRequest, ContinuousVariableMetadataSpec> {
  
  private static final Logger LOG = LogManager.getLogger(ContinuousVariablePlugin.class);

  private static final ManagedMap<String,String> RESULT_CACHE =
      new ManagedMap<>(5000, 2000);

  private String _cacheKey;
  private String _cachedResponse;

  @Override
  protected ClassGroup getTypeParameterClasses() {
    return new EmptyComputeClassGroup(ContinuousVariableMetadataPostRequest.class, ContinuousVariableMetadataSpec.class);
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
    String entityId = pluginSpec.getVariable().getEntityId();
    _cacheKey = EncryptionUtil.md5(
        _referenceMetadata.getStudyId() + "|" + entityId + "|" +
        JsonUtil.serializeObject(getSubsetFilters()) +
        "|" + JsonUtil.serializeObject(pluginSpec)
    );
    _cachedResponse = RESULT_CACHE.get(_cacheKey);
    if (_cachedResponse != null) LOG.info("Found cached result.");

    return _cachedResponse != null ? Collections.emptyList() : List.of(
      new StreamSpec(DEFAULT_SINGLE_STREAM_NAME, pluginSpec.getVariable().getEntityId())
        .addVar(pluginSpec.getVariable()));
  }

  @Override
  protected void writeResults(OutputStream out, Map<String, InputStream> dataStreams) throws IOException {
    if (_cachedResponse == null) {
      try {
        _cachedResponse = RESULT_CACHE.getValue(_cacheKey, key -> {
          LOG.info("Result not in cache; calculating..");
          JSONObject json = new JSONObject();

          PluginUtil util = getUtil();
          ContinuousVariableMetadataSpec spec = getPluginSpec();
          List<String> requestedMetadata = spec.getMetadata();

          useRConnectionWithRemoteFiles(Resources.RSERVE_URL, dataStreams, connection -> {
            connection.voidEval(util.getVoidEvalFreadCommand(DEFAULT_SINGLE_STREAM_NAME, spec.getVariable()));
            // for convenience
            connection.voidEval("x <- " + DEFAULT_SINGLE_STREAM_NAME + "$" + util.toColNameOrEmpty(spec.getVariable()));

            // Dates are read in as class=character. If we see the variable type is date, let's
            // force the data in R to be have class=date. See veupathUtils #14 for more details.
            if (util.getVariableType(spec.getVariable()).equals(APIVariableType.DATE.toString())) {
              connection.voidEval("x <- as.Date(x)");
            }

            if (requestedMetadata.contains("binRanges")) {
              // TODO add support for user-defined N bins? for now 10..
              String equalIntervalJson = connection.eval("veupathUtils::toJSON(veupathUtils::getDiscretizedBins(x, 'equalInterval', 10, FALSE), FALSE)").asString();
              String quantileJson = connection.eval("veupathUtils::toJSON(veupathUtils::getDiscretizedBins(x, 'quantile', 10, FALSE), FALSE)").asString();
              // sd bins return 6 bins at most, no user control supported in R currently
              String sdJson = connection.eval("veupathUtils::toJSON(veupathUtils::getDiscretizedBins(x, 'sd', NULL, FALSE), FALSE)").asString();
              json.put("binRanges", new JSONObject()
                  .put("equalInterval", new JSONArray(equalIntervalJson))
                  .put("quantile", new JSONArray(quantileJson))
                  .put("standardDeviation", new JSONArray(sdJson)));
            }

            if (requestedMetadata.contains("median")) {
              String quotedResult = connection.eval("jsonlite::toJSON(jsonlite::unbox(formatC(median(x, na.rm = TRUE))))").asString();
              double median = Double.parseDouble(quotedResult.substring(1,quotedResult.length() - 1));
              json.put("median", median);
            }

          });

          return json.toString();
        });
      }
      catch (ValueProductionException e) {
        throw new RuntimeException("Could not generate continuous variable metadata", e);
      }
    }
    out.write(_cachedResponse.getBytes(StandardCharsets.UTF_8));
    out.flush();
  }
}
