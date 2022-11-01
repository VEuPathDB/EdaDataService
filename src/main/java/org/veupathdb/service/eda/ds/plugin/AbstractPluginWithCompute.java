package org.veupathdb.service.eda.ds.plugin;

import java.util.List;
import jakarta.ws.rs.BadRequestException;
import org.gusdb.fgputil.validation.ValidationException;
import org.veupathdb.service.eda.common.client.spec.StreamSpec;
import org.veupathdb.service.eda.ds.metadata.AppsMetadata;
import org.veupathdb.service.eda.generated.model.APIStudyDetail;
import org.veupathdb.service.eda.generated.model.AppOverview;
import org.veupathdb.service.eda.generated.model.ComputeConfig;
import org.veupathdb.service.eda.generated.model.VisualizationRequestBase;

public abstract class AbstractPluginWithCompute<T extends VisualizationRequestBase, S, R extends ComputeConfig> extends AbstractPlugin<T, S> {

  protected static final String COMPUTE_STREAM_NAME = "compute_input_dataset";

  // stored and typed value of the passed compute config object
  private R _computeSpec;

  // returns the compute-spec class this visualization expects
  protected abstract Class<R> getComputeSpecClass();
  protected abstract List<StreamSpec> getRequestedStreams(S pluginSpec, R computeConfig);

  protected R getComputeConfig() {
    return _computeSpec;
  }

  @Override
  protected final List<StreamSpec> getRequestedStreams(S pluginSpec) {
    return getRequestedStreams(pluginSpec, _computeSpec);
  }

  @Override
  protected void loadAdditionalConfig(String appName, T request) throws ValidationException {
    // pull out the passed config spec and validate
    _computeSpec = getSpecObject(request, "getComputeConfig", getComputeSpecClass());
    validateComputeName(_computeSpec.getName(), appName);
  }

  private void validateComputeName(String passedComputeName, String appName) {
    AppsMetadata.APPS.getApps().stream()
        // find this app by name
        .filter(app -> app.getName().equals(appName)).findFirst()
        // look up compute associated with this app
        .map(AppOverview::getComputeName)
        // convert to empty optional if compute names do not match
        .filter(associatedCompute -> associatedCompute.equals(passedComputeName))
        // missing app or no match; throw exception
        .orElseThrow(() -> new BadRequestException(
            "Compute specified in compute config is not associated with this app."));
  }

}