package org.veupathdb.service.eda.ds.plugin;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gusdb.fgputil.functional.FunctionalInterfaces.ConsumerWithException;
import org.gusdb.fgputil.validation.ValidationBundle;
import org.gusdb.fgputil.validation.ValidationBundle.ValidationBundleBuilder;
import org.gusdb.fgputil.validation.ValidationException;
import org.veupathdb.service.eda.common.client.AbstractTabularDataClient;
import org.veupathdb.service.eda.common.client.ClientUtil;
import org.veupathdb.service.eda.common.client.EdaMergingClient;
import org.veupathdb.service.eda.common.client.EdaSubsettingClient;
import org.veupathdb.service.eda.common.client.StreamSpec;
import org.veupathdb.service.eda.common.model.EntityDef;
import org.veupathdb.service.eda.common.model.ReferenceMetadata;
import org.veupathdb.service.eda.ds.Resources;
import org.veupathdb.service.eda.generated.model.APIFilter;
import org.veupathdb.service.eda.generated.model.APIStudyDetail;
import org.veupathdb.service.eda.generated.model.APIVariableType;
import org.veupathdb.service.eda.generated.model.AnalysisRequestBase;
import org.veupathdb.service.eda.generated.model.DerivedVariable;
import org.veupathdb.service.eda.generated.model.VariableSpec;

abstract class AbstractPlugin<T extends AnalysisRequestBase, S> implements Consumer<OutputStream> {

  private static final Logger LOG = LogManager.getLogger(AbstractPlugin.class);

  protected abstract Class<S> getAnalysisSpecClass();
  protected abstract ValidationBundle validateAnalysisSpec(S pluginSpec) throws ValidationException;
  protected abstract List<StreamSpec> getRequestedStreams(S pluginSpec);
  protected abstract void writeResults(OutputStream out, Map<String, InputStream> dataStreams) throws IOException;

  private final EdaSubsettingClient _subsettingClient = new EdaSubsettingClient(Resources.SUBSETTING_SERVICE_URL);
  private final AbstractTabularDataClient _mergingClient = _subsettingClient; //new EdaMergingClient(Resources.MERGING_SERVICE_URL);

  private boolean _requestProcessed = false;
  private S _pluginSpec;
  private List<APIFilter> _subset;
  private List<DerivedVariable> _derivedVariables;
  private ReferenceMetadata _referenceMetadata;

  private List<StreamSpec> _requiredStreams;

  public final AbstractPlugin<T,S> processRequest(T request) throws ValidationException {

    // validate config type matches class provided by subclass
    _pluginSpec = getSpecObject(request);

    // check for subset and derived entity properties of request
    _subset = Optional.ofNullable(request.getFilters()).orElse(Collections.emptyList());
    _derivedVariables = Optional.ofNullable(request.getDerivedVariables()).orElse(Collections.emptyList());

    // get study
    APIStudyDetail study = _subsettingClient.getStudy(request.getStudyId())
        .orElseThrow(() -> new ValidationException("Study '" + request.getStudyId() + "' does not exist."));

    // construct available variables for each entity from metadata and derived variable config
    _referenceMetadata = new ReferenceMetadata(study, _derivedVariables);

    // ask subclass to validate the configuration
    validateAnalysisSpec(_pluginSpec).throwIfInvalid();

    // get list of data streams required by this subclass
    _requiredStreams = getRequestedStreams(_pluginSpec);

    // validate stream specs provided by the subclass
    _mergingClient.getStreamSpecValidator()
        .validateStreamSpecs(_requiredStreams, _referenceMetadata).throwIfInvalid();

    _requestProcessed = true;
    return this;
  }

  @Override
  public final void accept(OutputStream out) {
    if (!_requestProcessed) {
      throw new RuntimeException("Output cannot be streamed until request has been processed.");
    }

    // create stream generator
    Function<StreamSpec,InputStream> streamGenerator = spec -> _mergingClient
        .getTabularDataStream(_referenceMetadata, _subset, spec);

    // create stream processor
    ConsumerWithException<Map<String,InputStream>> streamProcessor = map -> writeResults(out, map);

    // build and process streams
    LOG.info("Building and processing " + _requiredStreams.size() + " required data streams.");
    ClientUtil.buildAndProcessStreams(_requiredStreams, streamGenerator, streamProcessor);
  }

  protected S getPluginSpec() {
    return _pluginSpec;
  }

  protected ReferenceMetadata getReferenceMetadata() {
    return _referenceMetadata;
  }

  protected String toColNameOrEmpty(VariableSpec var) {
    return var == null ? "" : _mergingClient.varToColumnHeader(var);
  }

  protected EntityDef getValidEntity(ValidationBundleBuilder validation, String entityId) throws ValidationException {
    return getReferenceMetadata().getValidEntity(validation, entityId);
  }

  protected void validateVariableNameAndType(ValidationBundleBuilder validation, EntityDef entity, String variableUse, VariableSpec varSpec, APIVariableType... allowedTypes) {
    getReferenceMetadata().validateVariableNameAndType(validation, entity, variableUse, varSpec, allowedTypes);
  }

  @SuppressWarnings("unchecked")
  private S getSpecObject(T request) {
    try {
      Method configGetter = request.getClass().getMethod("getConfig");
      Object config = configGetter.invoke(request);
      if (getAnalysisSpecClass().isAssignableFrom(config.getClass())) {
        return (S)config;
      }
      throw new RuntimeException("Plugin class " + getClass().getName() +
          " declares spec class "  + getAnalysisSpecClass().getName() +
          " but " + request.getClass().getName() + "::getConfig()" +
          " returned " + config.getClass().getName() + ". The second must be a subclass of the first.");
    }
    catch (NoSuchMethodException noSuchMethodException) {
      throw new RuntimeException("Generated class " + request.getClass().getName() +
          " must implement a no-arg method getConfig() which returns an instance of " + getAnalysisSpecClass().getName());
    }
    catch (IllegalAccessException | InvocationTargetException e) {
      throw new RuntimeException("Misconfiguration of analysis plugin: " + getClass().getName(), e);
    }
  }
}
