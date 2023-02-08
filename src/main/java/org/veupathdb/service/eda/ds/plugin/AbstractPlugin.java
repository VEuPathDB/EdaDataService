package org.veupathdb.service.eda.ds.plugin;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import jakarta.ws.rs.BadRequestException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gusdb.fgputil.Timer;
import org.gusdb.fgputil.Tuples.TwoTuple;
import org.gusdb.fgputil.client.ResponseFuture;
import org.gusdb.fgputil.functional.FunctionalInterfaces.ConsumerWithException;
import org.gusdb.fgputil.functional.Functions;
import org.gusdb.fgputil.json.JsonUtil;
import org.gusdb.fgputil.validation.ValidationException;
import org.veupathdb.service.eda.common.client.*;
import org.veupathdb.service.eda.common.client.EdaComputeClient.ComputeRequestBody;
import org.veupathdb.service.eda.common.client.spec.StreamSpec;
import org.veupathdb.service.eda.common.model.ReferenceMetadata;
import org.veupathdb.service.eda.common.plugin.constraint.ConstraintSpec;
import org.veupathdb.service.eda.common.plugin.constraint.DataElementSet;
import org.veupathdb.service.eda.common.plugin.constraint.DataElementValidator;
import org.veupathdb.service.eda.common.plugin.util.PluginUtil;
import org.veupathdb.service.eda.ds.Resources;
import org.veupathdb.service.eda.ds.metadata.AppsMetadata;
import org.veupathdb.service.eda.generated.model.*;
import org.veupathdb.service.eda.generated.model.BinSpec.RangeType;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.veupathdb.service.eda.common.plugin.util.PluginUtil.singleQuote;

/**
 * Base visualization plugin for all other plugins.  Provides access to parts of
 * the request object, manages logic flow over the course of the request, and
 * provides streaming merged data to subclasses for processing per specs provided
 * by those subclasses.
 *
 * @param <T> type of request (must extend VisualizationRequestBase)
 * @param <S> plugin's spec class (must be or extend the generated spec class for this plugin)
 * @param <R> plugin's compute spec class (must be or extend the generated compute spec class for this plugin)
 */
public abstract class AbstractPlugin<T extends VisualizationRequestBase, S, R> implements Consumer<OutputStream> {

  private static final Logger LOG = LogManager.getLogger(AbstractPlugin.class);

  // shared stream name for plugins that need request only a single stream
  protected static final String DEFAULT_SINGLE_STREAM_NAME = "single_tabular_dataset";

  // methods that need to be implemented
  protected abstract Class<T> getVisualizationRequestClass();
  protected abstract Class<S> getVisualizationSpecClass();
  protected abstract Class<R> getComputeConfigClass();
  protected abstract void validateVisualizationSpec(S pluginSpec) throws ValidationException;
  protected abstract List<StreamSpec> getRequestedStreams(S pluginSpec);

  // return true to include computed vars as part of the tabular stream
  //   for the entity under which they were computed.  If true, a runtime
  //   error will occur if no stream spec exists for that entity.
  protected abstract boolean includeComputedVarsInStream();

  protected abstract void writeResults(OutputStream out, Map<String, InputStream> dataStreams) throws IOException;

  // methods that should probably be overridden
  public String getDisplayName() { return getClass().getName(); }
  public String getDescription() { return ""; }
  public List<String> getProjects() { return Collections.emptyList(); }
  // have to decide if default is 1 and 25 override or vice versa. to facet or not, that is the question...
  public Integer getMaxPanels() { return 1; }
  public ConstraintSpec getConstraintSpec() { return new ConstraintSpec(); }

  protected ReferenceMetadata _referenceMetadata;
  protected S _pluginSpec;
  // stored compute name and typed value of the passed compute config object (if plugin requires compute)
  protected Optional<TwoTuple<String,R>> _computeInfo;
  protected List<StreamSpec> _requiredStreams;

  private Timer _timer;
  private boolean _requestProcessed = false;
  private String _studyId;
  private List<APIFilter> _subsetFilters;
  private List<DerivedVariableSpec> _derivedVariableSpecs;
  private Entry<String,String> _authHeader;
  private EdaSubsettingClient _subsettingClient;
  private EdaMergingClient _mergingClient;
  private EdaComputeClient _computeClient;

  /**
   * Processes the plugin request and prepares this plugin to receive an OutputStream via the
   * accept() method, to which it will write its response.  As much processing as possible should
   * be done in this method up front, since once accept() is called, we are tied to a 2xx response.
   *
   * @param appName app name this plugin belongs to; can be null if no compute is expected/allowed
   * @param request incoming plugin request object
   * @param authHeader authentication header observed on request
   * @return this
   * @throws ValidationException if request validation fails
   */
  public final AbstractPlugin<T,S,R> processRequest(String appName, T request, Entry<String,String> authHeader) throws ValidationException {

    // start request timer (used to profile request performance dynamics)
    _timer = new Timer();
    logRequestTime("Starting timer");

    // validate config types match classes provided by subclass
    _pluginSpec = getSpecObject(request, "getConfig", getVisualizationSpecClass());

    // find compute name if required by this viz plugin; if present, then look up compute config
    //   and create an optional tuple of name+config (empty optional if viz does not require compute)
    _computeInfo = appName == null ? Optional.empty() : findComputeName(appName).map(name -> new TwoTuple<>(name,
        getSpecObject(request, "getComputeConfig", getComputeConfigClass())));

    // check for subset and derived entity properties of request
    _subsetFilters = Optional.ofNullable(request.getFilters()).orElse(Collections.emptyList());
    _derivedVariableSpecs = Optional.ofNullable(request.getDerivedVariables()).orElse(Collections.emptyList());

    // build clients for required services
    _authHeader = authHeader;
    _subsettingClient = new EdaSubsettingClient(Resources.SUBSETTING_SERVICE_URL, authHeader);
    _mergingClient = new EdaMergingClient(Resources.MERGING_SERVICE_URL, authHeader);
    _computeClient = new EdaComputeClient(Resources.COMPUTE_SERVICE_URL, authHeader);

    // get study
    APIStudyDetail study = _subsettingClient.getStudy(request.getStudyId())
        .orElseThrow(() -> new ValidationException("Study '" + request.getStudyId() + "' does not exist."));
    _studyId = study.getId();

    // if plugin requires a compute, check if compute results are available
    if (_computeInfo.isPresent() && !isComputeResultsAvailable()) {
      throw new BadRequestException("Compute results are not available for the requested job.");
    }

    // if plugin requires a compute, get computed var metadata
    List<VariableMapping> computedVars = _computeInfo.isPresent()
        ? getComputedVariableMetadata().getVariables()
        : Collections.emptyList();

    // construct available variables for each entity from metadata and derived variable config
    _referenceMetadata = new ReferenceMetadata(study, computedVars, _derivedVariableSpecs);

    // ask subclass to validate the configuration
    validateVisualizationSpec(_pluginSpec);

    // get list of data streams required by this subclass
    _requiredStreams = getRequestedStreams(_pluginSpec);

    // validate stream specs provided by the subclass
    _mergingClient.getStreamSpecValidator()
        .validateStreamSpecs(_requiredStreams, _referenceMetadata).throwIfInvalid();

    _requestProcessed = true;
    logRequestTime("Initial request processing complete");
    return this;
  }

  protected void logRequestTime(String eventDescription) {
    LOG.info("Request Time: " + _timer.getElapsed() + "ms, " + eventDescription);
  }

  @Override
  public final void accept(OutputStream out) {
    if (!_requestProcessed) {
      throw new RuntimeException("Output cannot be streamed until request has been processed.");
    }

    // create stream generator
    Optional<TwoTuple<String,Object>> typedTuple = _computeInfo.map(info -> new TwoTuple<>(info.getFirst(), info.getSecond()));
    Function<StreamSpec, ResponseFuture> streamGenerator = spec -> _mergingClient
        .getTabularDataStream(_referenceMetadata, _subsetFilters, typedTuple, spec);

    // create stream processor
    // TODO: might make disallowing empty results optional in the future; this is the original implementation
    //ConsumerWithException<Map<String,InputStream>> streamProcessor = map -> writeResults(out, map);
    ConsumerWithException<Map<String,InputStream>> streamProcessor = map -> writeResults(out,
        Functions.mapValues(map, entry -> new NonEmptyResultStream(entry.getKey(), entry.getValue())));

    // build and process streams
    logRequestTime("Making requests for data streams");
    LOG.info("Building and processing " + _requiredStreams.size() + " required data streams.");
    StreamingDataClient.buildAndProcessStreams(_requiredStreams, streamGenerator, streamProcessor);
    logRequestTime("Data streams processed; response written; request complete");
  }

  protected S getPluginSpec() {
    return _pluginSpec;
  }

  protected String getStudyId() {
    return _studyId;
  }

  protected List<APIFilter> getSubsetFilters() {
    return _subsetFilters;
  }

  protected ReferenceMetadata getReferenceMetadata() {
    return _referenceMetadata;
  }

  protected PluginUtil getUtil() {
    return new PluginUtil(getReferenceMetadata(), _mergingClient);
  }

  protected void validateInputs(DataElementSet values) throws ValidationException {
    new DataElementValidator(getReferenceMetadata(), getConstraintSpec()).validate(values);
  }

  @SuppressWarnings("unchecked")
  private <Q> Q getSpecObject(T request, String methodName, Class<Q> specClass) {
    try {
      Method configGetter = request.getClass().getMethod(methodName);
      Object config = configGetter.invoke(request);
      if (specClass.isAssignableFrom(config.getClass())) {
        return (Q)config;
      }
      throw new RuntimeException("Plugin class " + getClass().getName() +
          " declares spec class "  + specClass.getName() +
          " but " + request.getClass().getName() + "::" + methodName + "()" +
          " returned " + config.getClass().getName() + ". The second must be a subclass of the first.");
    }
    catch (NoSuchMethodException noSuchMethodException) {
      throw new RuntimeException("Generated class " + request.getClass().getName() +
          " must implement a no-arg method " + methodName + "() which returns an instance of " + specClass.getName());
    }
    catch (IllegalAccessException | InvocationTargetException e) {
      throw new RuntimeException("Misconfiguration of visualization plugin: " + getClass().getName(), e);
    }
  }

  /**
   * Enables visualization plugins to call each other to get any required data.  The same study, subset filters, and
   * derived variable specs passed to this instance will be used to call the other plugin.  This process is has a number
   * of caveats:
   * 1. The call is synchronous, so the calling plugin blocks until the result is collected
   * 2. The result is put into a response object for that plugin and stored in memory, so should not be large
   * 3. The plugin to be called must currently use the same subset/filters as the calling plugin.  There has been talk
   *    of wanting to enable the target plugin to use a different subset than the calling plugin but the how and why of
   *    those use cases need to be worked out.
   * 4. The plugin to be called cannot have a compute.  In the future, we could maybe have it depend on the same
   *    compute (and compute config) as the calling plugin, but beyond that this plugin instance does not have enough
   *    information to make that call.
   *
   * @param plugin plugin to call
   * @param visualizationConfig configuration object specific to this plugin (i.e. the spec, normally attached to the config JSON property)
   * @param expectedResponseType expected response type of the plugin to be called.  This can be either the interface or implementation class of the response type
   * @param <U> type of request body for the plugin
   * @param <V> type of the config object for the plugin (set on the undeclared config property of U)
   * @param <W> type of response generated by the plugin to be called
   * @return response generated by the called plugin
   */
  protected <U extends VisualizationRequestBase, V, W> W invokePlugin(
      AbstractEmptyComputePlugin<U,V> plugin, V visualizationConfig, Class<W> expectedResponseType) {
    try {
      // build plugin request
      String requestClassName = plugin.getVisualizationRequestClass().getName();
      // need to use the implementation class (not the interface) so we can instantiate it
      if (!requestClassName.endsWith("Impl")) requestClassName += "Impl";
      U request = (U)Class.forName(requestClassName).getConstructor().newInstance();
      request.setStudyId(_studyId);
      request.setFilters(_subsetFilters);
      request.setDerivedVariables(_derivedVariableSpecs); // probably not needed; how are these relevant?
      // config is not a property on VisualizationRequestBase, but we always expect it; invoke with the passed config
      plugin.getVisualizationRequestClass().getMethod("setConfig", plugin.getVisualizationSpecClass()).invoke(request, visualizationConfig);
      ByteArrayOutputStream result = new ByteArrayOutputStream();
      // passing null as appName because it is only used to look up the compute; since this is an EmptyComputePlugin, we know there is no compute
      plugin.processRequest(null, request, _authHeader).accept(result);
      // need to use the implementation class (not the interface) so Jackson can instantiate it
      String expectedResponseTypeName = expectedResponseType.getName();
      if (!expectedResponseTypeName.endsWith("Impl")) expectedResponseTypeName += "Impl";
      return (W)JsonUtil.Jackson.readValue(result.toString(), Class.forName(expectedResponseTypeName));
    }
    catch (ClassNotFoundException | NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
      throw new RuntimeException("Unable to create request body object", e);
    }
    catch (JsonProcessingException e) {
      throw new RuntimeException("Unable to convert plugin response into expected response type", e);
    }
    catch (ValidationException e) {
      throw new RuntimeException("Attempt to invoke plugin internally with invalid config", e);
    }
  }

  /*****************************************************************
   *** Compute-related methods
   ****************************************************************/

  private static Optional<String> findComputeName(String appName) {
    return Optional.ofNullable(AppsMetadata.APPS.getApps().stream()
        // find this app by name
        .filter(app -> app.getName().equals(appName)).findFirst().orElseThrow()
        // look up compute associated with this app
        .getComputeName());
  }

  private final Supplier<RuntimeException> NO_COMPUTE_EXCEPTION = () ->
      new UnsupportedOperationException("This visualization plugin [" +
          getClass().getSimpleName() + "] is not associated with a compute plugin");

  /**
   * @return the compute name used by this plugin; should only be called if plugin requires a compute
   * @throws NoSuchElementException if not associated with a compute
   */
  protected String getComputeName() {
    return _computeInfo.map(TwoTuple::getFirst).orElseThrow(NO_COMPUTE_EXCEPTION);
  }

  /**
   * @return the compute config for this plugin; should only be called if plugin requires a compute
   * @throws NoSuchElementException if no compute config is present
   */
  protected R getComputeConfig() {
    return _computeInfo.map(TwoTuple::getSecond).orElseThrow(NO_COMPUTE_EXCEPTION);
  }

  protected boolean isComputeResultsAvailable() {
    return _computeClient.isJobResultsAvailable(getComputeName(), createComputeRequestBody());
  }

  protected <Q> Q getComputeResultStats(Class<Q> expectedStatsClass) {
    return _computeClient.getJobStatistics(getComputeName(), createComputeRequestBody(), expectedStatsClass);
  }

  protected ComputedVariableMetadata getComputedVariableMetadata() {
    return _computeClient.getJobVariableMetadata(getComputeName(), createComputeRequestBody());
  }

  private ComputeRequestBody createComputeRequestBody() {
    return new ComputeRequestBody(
        _studyId,
        _subsetFilters,
        _derivedVariableSpecs,
        _computeInfo.map(TwoTuple::getSecond).orElseThrow(NO_COMPUTE_EXCEPTION));
  }

  /*****************************************************************
   *** Shared plugin-specific utilities
   ****************************************************************/

  protected void validateBinSpec(BinSpec binSpec, String xVarType) {
    if (xVarType.equals("NUMBER") || xVarType.equals("INTEGER")) {
      if (binSpec.getUnits() != null) {
        LOG.warn("The `units` property of the `BinSpec` class is only used for DATE x-axis variables. It will be ignored.");
      }
    }
    // need an error here if it's a date and we don't have a unit?
  }

  public static String getBinRangeAsRString(RangeType binRange) {
    if (binRange != null) {
      if (binRange.isNumberRange()) {
        return(getBinRangeAsRString(binRange.getNumberRange()));
      } else {
        return(getBinRangeAsRString(binRange.getDateRange()));
      }
    } else {
      return("binRange <- NULL");
    }
  }

  public static String getBinRangeAsRString(NumberRange binRange) {
    if (binRange != null) {
      return("binRange <- list('min'=" + binRange.getMin().toString() + ", 'max'=" + binRange.getMax().toString() + ")");
    } else {
      return("binRange <- NULL");
    }
  }

  public static String getBinRangeAsRString(DateRange binRange) {
    if (binRange != null) {
      return("binRange <- list('min'='" + binRange.getMin() + "', 'max'='" + binRange.getMax() + "')");
    } else {
      return("binRange <- NULL");
    }
  }

  public static String getViewportAsRString(NumericViewport viewport, String xVarType) {
    if (viewport != null) {
      // think if we just pass the string plot.data will convert it to the claimed type
      if (xVarType.equals("NUMBER") || xVarType.equals("INTEGER")) {
        return("viewport <- list('xMin'=" + viewport.getXMin() + ", 'xMax'=" + viewport.getXMax() + ")");
      } else {
        return("viewport <- list('xMin'=" + singleQuote(viewport.getXMin()) + ", 'xMax'=" + singleQuote(viewport.getXMax()) + ")");
      }
    } else {
      return("viewport <- NULL");
    }
  }

  public static String getViewportAsRString(GeolocationViewport viewport) {
    if (viewport != null) {
      return("viewport <- list('latitude'=list('xMin'= " + viewport.getLatitude().getXMin() +
          ", 'xMax'= " + viewport.getLatitude().getXMax() +
          "), 'longitude'= list('left'= " + viewport.getLongitude().getLeft() +
          ", 'right' = " + viewport.getLongitude().getRight() + "))");
    } else {
      return("viewport <- NULL");
    }
  }

  // there is probably some JRI util that would make this unnecessary if i were more clever??
  public static String listToRVector(List<String> values) {
    return
        "c(" +
        values.stream()
            .map(PluginUtil::doubleQuote)
            .collect(Collectors.joining(", ")) +
        ")";
  }

  public String getVariableMetadataRObjectAsString(VariableMapping var) {
    if (var == null) return null;

    StringBuilder variableMetadata = new StringBuilder(
      "veupathUtils::VariableMetadata(" +
      "variableClass=veupathUtils::VariableClass(value='computed')," +
      "variableSpec=veupathUtils::VariableSpec(variableId=" + singleQuote(var.getVariableSpec().getVariableId()) + ",entityId=" + singleQuote(var.getVariableSpec().getEntityId()) + ")," +
      "plotReference=veupathUtils::PlotReference(value=" + singleQuote(var.getPlotReference().getValue()) + ")," +
      "dataType=veupathUtils::DataType(value=" + singleQuote(var.getDataType().toString()) + ")," +
      "dataShape=veupathUtils::DataShape(value=" + singleQuote(var.getDataShape().toString()) + ")," +
      "imputeZero=" + var.getImputeZero().toString().toUpperCase() + "," +
      "isCollection=" + var.getIsCollection().toString().toUpperCase()
    );

    if (var.getDisplayName() != null)
      variableMetadata.append(",displayName=" + singleQuote(var.getDisplayName()));

    if (var.getDisplayRangeMax() != null && var.getDisplayRangeMin() != null) {
      variableMetadata.append(
          var.getDataType() == APIVariableType.DATE
          ? ",displayRangeMin=" + singleQuote(var.getDisplayRangeMin().toString()) + ",displayRangeMax=" + singleQuote(var.getDisplayRangeMax().toString())
          : ",displayRangeMin=" + var.getDisplayRangeMin().toString() + ",displayRangeMax=" + var.getDisplayRangeMax().toString()
      );
    }

    if (var.getVocabulary() != null)
      variableMetadata.append(",vocabulary=" + listToRVector(var.getVocabulary()));

    if (var.getMembers() != null)
      variableMetadata.append(",members=" + getVariableSpecListRObjectAsString(var.getMembers()));

    return variableMetadata.append(")").toString();
  }

  public String getVoidEvalComputedVariableMetadataList(ComputedVariableMetadata metadata) {
    return
        "computedVariables <- veupathUtils::VariableMetadataList(S4Vectors::SimpleList(" +
        metadata.getVariables().stream()
            .map(this::getVariableMetadataRObjectAsString)
            .filter(Objects::nonNull)
            .collect(Collectors.joining(",")) +
        "))";
  }

  public String getVariableSpecRObjectAsString(VariableSpec var) {
    return var == null ? null :
        "veupathUtils::VariableSpec(variableId=" + singleQuote(var.getVariableId()) + ",entityId=" + singleQuote(var.getEntityId()) + ")";
  }

  public String getVariableSpecListRObjectAsString(List<? extends VariableSpec> vars) {
    return vars == null ? null :
        "veupathUtils::VariableSpecList(S4Vectors::SimpleList(" +
        vars.stream()
            .map(this::getVariableSpecRObjectAsString)
            .collect(Collectors.joining(",")) +
        "))";
  }

  public String getVariableMetadataRObjectAsString(CollectionSpec collection, String plotReference) {
    if (collection == null) return null;
    PluginUtil util = getUtil();
    String membersList = getVariableSpecListRObjectAsString(util.getCollectionMembers(collection));
    return
        "veupathUtils::VariableMetadata(" +
        "variableClass=veupathUtils::VariableClass(value='native')," +
        "variableSpec=veupathUtils::VariableSpec(variableId=" + singleQuote(collection.getCollectionId()) + ",entityId=" + singleQuote(collection.getEntityId()) + ")," +
        "plotReference=veupathUtils::PlotReference(value=" + singleQuote(plotReference) + ")," +
        "dataType=" + singleQuote(util.getCollectionType(collection)) + "," +
        "dataShape=" + singleQuote(util.getCollectionDataShape(collection)) + "," +
        "imputeZero=" + util.getCollectionImputeZero(collection).toUpperCase() + "," +
        "isCollection=TRUE," +
        "members=" + membersList + ")";
  }

  public String getVariableMetadataRObjectAsString(VariableSpec var, String plotReference) {
    if (var == null) return null;
    PluginUtil util = getUtil();
    return
        "veupathUtils::VariableMetadata(" +
        "variableClass=veupathUtils::VariableClass(value='native')," +
        "variableSpec=veupathUtils::VariableSpec(variableId=" + singleQuote(var.getVariableId()) + ",entityId=" + singleQuote(var.getEntityId()) + ")," +
        "plotReference=veupathUtils::PlotReference(value=" + singleQuote(plotReference) + ")," +
        "dataType=veupathUtils::DataType(value=" + singleQuote(util.getVariableType(var)) + ")," +
        "dataShape=veupathUtils::DataShape(value=" + singleQuote(util.getVariableDataShape(var)) + ")," +
        "imputeZero=" + util.getVariableImputeZero(var).toUpperCase() + ")";
  }

  public String getVoidEvalVariableMetadataList(Map<String, VariableSpec> vars) {
    return
        // special case if vars is null or all var values are null
        vars == null || vars.values().stream().allMatch(Objects::isNull)
        ? "variables <- veupathUtils::VariableMetadataList()"

        // otherwise build R-friendly var list
        : "variables <- veupathUtils::VariableMetadataList(S4Vectors::SimpleList(" +
          vars.entrySet().stream()
            .map(entry -> {
              String plotReference = entry.getKey();
              VariableSpec var = entry.getValue();
              return getVariableMetadataRObjectAsString(var, plotReference);
            })
            .filter(Objects::nonNull)
            .collect(Collectors.joining(",")) +
          "))";
  }

}
