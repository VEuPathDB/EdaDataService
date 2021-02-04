package org.veupathdb.service.edads.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gusdb.fgputil.AutoCloseableList;
import org.gusdb.fgputil.FormatUtil;
import org.gusdb.fgputil.IoUtil;
import org.gusdb.fgputil.functional.FunctionalInterfaces.ConsumerWithException;
import org.gusdb.fgputil.validation.ValidationBundle;
import org.gusdb.fgputil.validation.ValidationBundle.ValidationBundleBuilder;
import org.gusdb.fgputil.validation.ValidationException;
import org.gusdb.fgputil.validation.ValidationLevel;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RFileOutputStream;
import org.veupathdb.service.edads.Resources;
import org.veupathdb.service.edads.generated.model.APIFilter;
import org.veupathdb.service.edads.generated.model.APIStudyDetail;
import org.veupathdb.service.edads.generated.model.APIVariableType;
import org.veupathdb.service.edads.generated.model.AnalysisRequestBase;
import org.veupathdb.service.edads.generated.model.DerivedVariable;
import org.veupathdb.service.edads.generated.model.VariableSpec;

public abstract class AbstractEdadsPlugin<T extends AnalysisRequestBase, S> implements Consumer<OutputStream> {

  private static final Logger LOG = LogManager.getLogger(AbstractEdadsPlugin.class);

  protected abstract Class<S> getAnalysisSpecClass();
  protected abstract ValidationBundle validateAnalysisSpec(S pluginSpec) throws ValidationException;
  protected abstract List<StreamSpec> getRequestedStreams(S pluginSpec);
  protected abstract void writeResults(OutputStream out, Map<String, InputStream> dataStreams) throws IOException;

  private boolean _requestProcessed = false;
  private S _pluginSpec;
  private APIStudyDetail _study;
  private List<APIFilter> _subset;
  private List<DerivedVariable> _derivedVariables;
  private Map<String, EntityDef> _supplementedEntityMap;
  private List<StreamSpec> _requiredStreams;

  public final AbstractEdadsPlugin<T,S> processRequest(T request) throws ValidationException {

    // validate config type matches class provided by subclass
    _pluginSpec = getSpecObject(request);

    // validate requested study exists and fetch metadata
    _study = EdaClient.getStudy(request.getStudyId());

    // check for subset and derived entity properties of request
    _subset = Optional.ofNullable(request.getFilters()).orElse(Collections.emptyList());
    _derivedVariables = Optional.ofNullable(request.getDerivedVariables()).orElse(Collections.emptyList());

    // construct available variables for each entity from metadata and derived variable config
    _supplementedEntityMap = VariableManagement.supplementEntities(_study, _derivedVariables);

    // ask subclass to validate the configuration
    validateAnalysisSpec(_pluginSpec).throwIfInvalid();

    // get list of data streams required by this subclass
    _requiredStreams = getRequestedStreams(_pluginSpec);

    // validate stream specs provided by the subclass
    validateStreamSpecs(_requiredStreams).throwIfInvalid();

    // setup complete; return this object on which accept(OutputStream) will be
    // called.  Required streams will be opened and passed to the subclass, which
    // will interpret them as needed and write the result to the given OutputStream
    _requestProcessed = true;
    return this;
  }

  protected String getStudyId() {
    return _study.getId();
  }

  protected Map<String, EntityDef> getEntityMap() {
    return _supplementedEntityMap;
  }

  protected S getPluginSpec() {
    return _pluginSpec;
  }

  private ValidationBundle validateStreamSpecs(List<StreamSpec> requestedStreams) {
    ValidationBundleBuilder validation = ValidationBundle.builder(ValidationLevel.RUNNABLE);
    Set<String> specNames = requestedStreams.stream().map(StreamSpec::getStreamName).collect(Collectors.toSet());
    if (specNames.size() != requestedStreams.size()) {
      validation.addError("Stream specs must not duplicate names.");
    }
    for (StreamSpec spec : requestedStreams) {
      EntityDef entity = _supplementedEntityMap.get(spec.getEntityId());
      if (entity == null) {
        validation.addError("Entity " + spec.getEntityId() + " does not exist in study " + _study.getId());
      }
      else {
        for (VariableSpec requestedVar : spec) {
          if (!entity.hasVariable(requestedVar)) {
            validation.addError("Entity " + entity.getId() + " does not support variable " + JsonUtil.serialize(requestedVar));
          }
        }
      }
    }
    return validation.build();
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
    catch(IllegalAccessException | InvocationTargetException e) {
      throw new RuntimeException("Misconfiguration of analysis plugin: " + getClass().getName(), e);
    }
  }

  @Override
  public final void accept(OutputStream out) {
    try {
      if (!_requestProcessed) {
        throw new RuntimeException("Output cannot be streamed until request has been processed.");
      }
      LOG.info("Building " + _requiredStreams.size() + " required data streams.");
      try(AutoCloseableList<InputStream> dataStreams = buildDataStreams()) {
        Map<String,InputStream> streamMap = new LinkedHashMap<>();
        for (int i = 0; i < dataStreams.size(); i++) {
          streamMap.put(_requiredStreams.get(i).getStreamName(), dataStreams.get(i));
        }
        writeResults(out, streamMap);
      }
    }
    catch (IOException e) {
      throw new RuntimeException("Unable to stream results", e);
    }
  }

  private AutoCloseableList<InputStream> buildDataStreams() {
    AutoCloseableList<InputStream> dataStreams = new AutoCloseableList<>();
    try {
      for (StreamSpec spec : _requiredStreams) {
        dataStreams.add(EdaClient.getDataStream(
            _study,
            _subset,
            _derivedVariables,
            spec
        ));
      }
      return dataStreams;
    }
    catch (Exception e) {
      // if exception occurs while creating streams; close any that successfully opened, then throw
      dataStreams.close();
      throw new RuntimeException("Unable to fetch all required data", e);
    }
  }

  protected EntityDef getValidEntity(ValidationBundleBuilder validation, String entityId) throws ValidationException {
    EntityDef entity = getEntityMap().get(entityId);
    if (entity == null) {
      validation.addError("No entity exists on study '" + getStudyId() + "' with ID '" + entityId + "'.");
      validation.build().throwIfInvalid();
    }
    return entity;
  }

  protected static void validateVariableName(ValidationBundleBuilder validation,
      EntityDef entity, String variableUse, VariableSpec variable) {
    List<APIVariableType> nonCategoryTypes = Arrays.stream(APIVariableType.values())
        .filter(type -> !type.equals(APIVariableType.CATEGORY))
        .collect(Collectors.toList());
    validateVariableNameAndType(validation, entity, variableUse, variable, nonCategoryTypes.toArray(new APIVariableType[0]));
  }

  protected static void validateVariableNameAndType(ValidationBundleBuilder validation,
      EntityDef entity, String variableUse, VariableSpec varSpec, APIVariableType... allowedTypes) {
    List<APIVariableType> allowedTypesList = Arrays.asList(allowedTypes);
    if (allowedTypesList.contains(APIVariableType.CATEGORY)) {
      throw new RuntimeException("Plugin should not be using categories as variables.");
    }
    String varDesc = "Variable " + JsonUtil.serialize(varSpec) + ", used for " + variableUse + ", ";
    if (!entity.hasVariable(varSpec)) {
      validation.addError(varDesc + "does not exist in entity " + entity.getId());
    }
    else if (!allowedTypesList.contains(entity.getVariable(varSpec).getType())) {
      validation.addError(varDesc + "must be one of the following types: " + FormatUtil.join(allowedTypes, ", "));
    }
  }

  protected static String toColNameOrEmpty(VariableSpec var) {
    return var == null ? "" : VariableDef.toColumnName(var);
  }

  protected static void useRConnection(ConsumerWithException<RConnection> consumer) {
    RConnection c = null;
    try {
      String rServeUrlStr = Resources.RSERVE_URL;
      URL rServeUrl = new URL(rServeUrlStr);
      c = new RConnection(rServeUrl.getHost(), rServeUrl.getPort());
      consumer.accept(c);
    }
    catch (Exception e) {
      throw new RuntimeException("Unable to complete processing", e);
    }
    finally {
      if (c != null) {
        c.close();
      }
    }
  }

  protected static void useRConnectionWithRemoteFiles(Map<String, InputStream> dataStreams, ConsumerWithException<RConnection> consumer) {
    useRConnection(connection -> {
      try {
        for (Entry<String, InputStream> stream : dataStreams.entrySet()) {
          RFileOutputStream dataset = connection.createFile(stream.getKey());
          IoUtil.transferStream(dataset, stream.getValue());
          dataset.close();
        }
        // all files written; consumer may now use them in its RServe call
        consumer.accept(connection);
      }
      finally {
        for (String name : dataStreams.keySet()) {
          connection.removeFile(name);
        }
      }
    });
  }
}
