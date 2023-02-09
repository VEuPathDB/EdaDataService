package org.veupathdb.service.eda.ds.plugin.sample;

import org.gusdb.fgputil.DelimitedDataParser;
import org.gusdb.fgputil.json.JsonUtil;
import org.gusdb.fgputil.validation.ValidationException;
import org.veupathdb.service.eda.common.client.spec.StreamSpec;
import org.veupathdb.service.eda.common.plugin.util.PluginUtil;
import org.veupathdb.service.eda.ds.plugin.AbstractPlugin;
import org.veupathdb.service.eda.generated.model.*;

import java.io.*;
import java.util.List;
import java.util.Map;

public class ExampleComputeVizPlugin extends AbstractPlugin<ExampleComputeVizPostRequest, ExampleComputeVizSpec, ExampleComputeConfig> {

  @Override
  protected Class<ExampleComputeVizPostRequest> getVisualizationRequestClass() {
    return ExampleComputeVizPostRequest.class;
  }

  @Override
  protected Class<ExampleComputeConfig> getComputeConfigClass() {
    return ExampleComputeConfig.class;
  }

  @Override
  protected Class<ExampleComputeVizSpec> getVisualizationSpecClass() {
    return ExampleComputeVizSpec.class;
  }

  @Override
  protected boolean includeComputedVarsInStream() {
    // this visualization uses a compute plugin that computes a variable (not just statistics)
    return true;
  }

  @Override
  protected void validateVisualizationSpec(ExampleComputeVizSpec pluginSpec) throws ValidationException {
    String computeEntityId = getComputeConfig().getInputVariable().getEntityId();
    if (!pluginSpec.getPrefixVar().getEntityId().equals(computeEntityId)) {
      throw new ValidationException("The prefix variable must be native to the same entity as the example compute");
    }
  }

  @Override
  protected List<StreamSpec> getRequestedStreams(ExampleComputeVizSpec pluginSpec) {
    return List.of(new StreamSpec(DEFAULT_SINGLE_STREAM_NAME, getComputeConfig().getInputVariable().getEntityId())
        .addVar(pluginSpec.getPrefixVar())
        .setIncludeComputedVars(true));
  }

  @Override
  protected void writeResults(OutputStream out, Map<String, InputStream> dataStreams) throws IOException {

    // get compute stats
    ExamplePluginStats stats = getComputeResultStats(ExamplePluginStats.class);
    int numComputedEmptyValues = stats.getNumEmptyValues();

    // get metadata to find computed column
    ComputedVariableMetadata metadata = getComputedVariableMetadata();
    VariableSpec computedVarSpec = metadata.getVariables().stream()
        .filter(var -> var.getPlotReference() == PlotReferenceValue.XAXIS)
        .findFirst().orElseThrow().getVariableSpec();

    // loop through computed var to find longest and average size of concatenated values
    PluginUtil util = getUtil();
    String longestValue = "";
    int numValues = 0;
    double sumOfLengths = 0;
    String prefixVarColumnName = util.toColNameOrEmpty(getPluginSpec().getPrefixVar());
    String computedVarColumnName = util.toColNameOrEmpty(computedVarSpec);
    try (BufferedReader in = new BufferedReader(new InputStreamReader(dataStreams.get(DEFAULT_SINGLE_STREAM_NAME)))) {
      if (!in.ready())
        throw new IllegalStateException("Incoming stream has no header/no data");
      DelimitedDataParser parser = new DelimitedDataParser(in.readLine(), "\t", true);
      while (in.ready()) {
        Map<String,String> row = parser.parseLine(in.readLine());
        String prefixValue = row.get(prefixVarColumnName);
        String computedValue = row.get(computedVarColumnName);
        String concatenation = prefixValue + computedValue;
        numValues++;
        sumOfLengths += concatenation.length();
        if (longestValue.length() < concatenation.length()) {
          longestValue = concatenation;
        }
      }
    }

    // get count stat from neighboring plugin
    RecordCountSpec countRequest = new RecordCountSpecImpl();
    countRequest.setEntityId(getComputeConfig().getInputVariable().getEntityId());
    RecordCountPostResponse countResponse = invokePlugin(new RecordCountPlugin(), countRequest, RecordCountPostResponse.class);

    // write response (use buffering for larger responses)
    ExampleComputeVizPostResponse response = new ExampleComputeVizPostResponseImpl();
    response.setCountPluginResult(countResponse.getRecordCount());
    response.setNumEmptyValues(numComputedEmptyValues);
    response.setLongestConcatenatedValue(longestValue);
    response.setAvgConcatenatedLength(numValues == 0 ? 0 : sumOfLengths / numValues);
    out.write(JsonUtil.serializeObject(response).getBytes());
    out.flush();
  }

}
