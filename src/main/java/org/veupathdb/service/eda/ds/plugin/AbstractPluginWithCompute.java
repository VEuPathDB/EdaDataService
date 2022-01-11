package org.veupathdb.service.eda.ds.plugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import javax.ws.rs.BadRequestException;
import org.gusdb.fgputil.functional.TreeNode;
import org.gusdb.fgputil.validation.ValidationException;
import org.veupathdb.service.eda.common.client.spec.StreamSpec;
import org.veupathdb.service.eda.common.model.EntityDef;
import org.veupathdb.service.eda.common.model.ReferenceMetadata;
import org.veupathdb.service.eda.common.model.VariableDef;
import org.veupathdb.service.eda.ds.metadata.AppsMetadata;
import org.veupathdb.service.eda.generated.model.APIStudyDetail;
import org.veupathdb.service.eda.generated.model.AppOverview;
import org.veupathdb.service.eda.generated.model.ComputeConfig;
import org.veupathdb.service.eda.generated.model.VariableSpec;
import org.veupathdb.service.eda.generated.model.VisualizationRequestBase;

public abstract class AbstractPluginWithCompute<T extends VisualizationRequestBase, S, R extends ComputeConfig> extends AbstractPlugin<T, S> {

  // stored and typed value of the passed compute config object
  private R _computeSpec;

  // returns the compute-spec class this visualization expects
  protected abstract Class<R> getComputeSpecClass();
  protected List<StreamSpec> getRequestedStreams(S pluginSpec) {
    return getRequestedStreams(pluginSpec, _computeSpec);
  }
  protected abstract List<StreamSpec> getRequestedStreams(S pluginSpec, R computeConfig);

  protected static final String COMPUTE_STREAM_NAME = "compute_input_dataset";

  @Override
  public AbstractPlugin<T,S> processRequest(String appName, T request, Entry<String,String> authHeader) throws ValidationException {

    // pull out the passed config spec
    _computeSpec = getSpecObject(request, "getComputeConfig", getComputeSpecClass());
    validateComputeName(_computeSpec.getName(), appName);

    // extract plugin spec
    _pluginSpec = getSpecObject(request, "getConfig", getVisualizationSpecClass());
      
    System.out.println(_computeSpec.getName());
    // _requiredStreams = getRequestedStreams(_pluginSpec, _computeSpec);
    System.out.println("getRequestedStreams success");

    return super.processRequest(appName, request, authHeader);
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

  protected R getComputeConfig() {
    return _computeSpec;
  }

  protected VariableSpec getComputeEntityIdVarSpec(String entityId) {
    VariableDef idColVariableDef = _referenceMetadata.getEntity(entityId).get().getIdColumnDef();

    return idColVariableDef;
  }
}