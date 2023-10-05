package org.veupathdb.service.eda.ds.plugin.sample;

import org.gusdb.fgputil.json.JsonUtil;
import org.gusdb.fgputil.validation.ValidationException;
import org.veupathdb.service.eda.common.client.spec.StreamSpec;
import org.veupathdb.service.eda.ds.core.AbstractEmptyComputePlugin;
import org.veupathdb.service.eda.generated.model.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class CategoricalDistributionPlugin extends AbstractEmptyComputePlugin<CategoricalDistributionPostRequest, VariableSpec> {

  @Override
  protected ClassGroup getTypeParameterClasses() {
    return new EmptyComputeClassGroup(CategoricalDistributionPostRequest.class, VariableSpec.class);
  }

  @Override
  protected void validateVisualizationSpec(VariableSpec variableSpec) throws ValidationException {
    // would rather see what happens when plugin fails to validate and subsetting client must handle
  }

  @Override
  protected List<StreamSpec> getRequestedStreams(VariableSpec pluginSpec) {
    return Collections.emptyList();
  }

  @Override
  protected void writeResults(OutputStream out, Map<String, InputStream> dataStreams) throws IOException {
    CategoricalDistributionPostResponse response = new CategoricalDistributionPostResponseImpl();
    response.setCountDistribution(toBinList(getCategoricalCountDistribution(_pluginSpec)));
    response.setProportionDistribution(toBinList(getCategoricalProportionDistribution(_pluginSpec)));
    JsonUtil.Jackson.writeValue(out, response);
  }

  private static List<CategoricalDistributionBin> toBinList(Map<String, ? extends Number> distributionResponse) {
    return distributionResponse.entrySet().stream().map(entry -> {
      CategoricalDistributionBin bin = new CategoricalDistributionBinImpl();
      bin.setLabel(entry.getKey());
      bin.setValue(entry.getValue());
      return bin;
    }).toList();
  }
}
