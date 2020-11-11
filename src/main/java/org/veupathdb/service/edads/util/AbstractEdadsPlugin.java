package org.veupathdb.service.edads.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.gusdb.fgputil.AutoCloseableList;
import org.gusdb.fgputil.validation.ValidationBundle;
import org.gusdb.fgputil.validation.ValidationException;
import org.gusdb.fgputil.validation.ValidationLevel;
import org.veupathdb.service.edads.generated.model.APIEntity;
import org.veupathdb.service.edads.generated.model.APIFilter;
import org.veupathdb.service.edads.generated.model.APIStudyDetail;
import org.veupathdb.service.edads.generated.model.APIVariableType;
import org.veupathdb.service.edads.generated.model.BaseAnalysisConfig;
import org.veupathdb.service.edads.generated.model.DerivedVariable;

public abstract class AbstractEdadsPlugin<T extends BaseAnalysisConfig, S> implements Consumer<OutputStream> {

  protected abstract Class<S> getConfigurationClass();
  protected abstract ValidationBundle validateConfig(S pluginSpec);
  protected abstract List<StreamSpec> getRequestedStreams(S pluginSpec);
  protected abstract void writeResults(OutputStream out, List<InputStream> dataStreams) throws IOException;

  private boolean _requestProcessed = false;
  private S _pluginSpec;
  private APIStudyDetail _study;
  private Optional<List<APIFilter>> _subset;
  private Optional<List<DerivedVariable>> _derivedVariables;
  private Map<String, EntityDef> _supplementedEntityMap;
  private List<StreamSpec> _requiredStreams;

  public final AbstractEdadsPlugin<T,S> processRequest(T request) throws ValidationException {

    // validate config type matches class provided by subclass
    _pluginSpec = getSpecObject(request);

    // validate requested study exists and fetch metadata
    _study = EdaClient.getStudy(request.getStudyId());

    // check for subset and derived entity properties of request
    _subset = Optional.ofNullable(request.getSubset());
    _derivedVariables = Optional.ofNullable(request.getDerivedVariables());

    // construct available variables for each entity from metadata and derived variable config
    _supplementedEntityMap = supplementEntities(_study.getRootEntity(), _derivedVariables);

    // ask subclass to validate the configuration
    validateConfig(_pluginSpec).throwIfInvalid();

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

  private ValidationBundle validateStreamSpecs(List<StreamSpec> requestedStreams) {
    ValidationBundle.ValidationBundleBuilder validation = ValidationBundle.builder(ValidationLevel.RUNNABLE);
    for (StreamSpec spec : requestedStreams) {
      EntityDef entity = _supplementedEntityMap.get(spec.getEntityId());
      if (entity == null) {
        validation.addError("Entity " + spec.getEntityId() + " does not exist in study " + _study.getId());
      }
      else {
        for (String requestedVar : spec) {
          if (!entity.containsKey(requestedVar)) {
            validation.addError("Entity " + entity.getId() + " does not contain variable " + requestedVar);
          }
        }
      }
    }
    return validation.build();
  }

  // can erase tree structure here since we only care about variables available on a particular entity
  private static Map<String, EntityDef> supplementEntities(APIEntity entity, Optional<List<DerivedVariable>> derivedVariables) {
    Map<String, EntityDef> entities = new HashMap<>();
    EntityDef entityDef = new EntityDef(entity.getId());

    // add variables for this entity
    entity.getVariables().stream()
      .filter(var -> !var.getType().equals(APIVariableType.CATEGORY))
      .map(var -> new VariableDef(var.getId(), var.getType()))
      .forEach(vd -> entityDef.put(vd.getId(), vd));

    // add derived variables for this entity
    derivedVariables
      .map(list -> list.stream()
        .filter(dr -> dr.getEntityId().equals(entity.getId()))
        .map(dr -> new VariableDef(dr.getVariableId(), dr.getType()))
        .filter(vd -> !entityDef.containsKey(vd.getId())) // skip if entity already contains the variable; will throw later
        .collect(Collectors.toList()))
      .orElse(Collections.emptyList())
      .forEach(vd -> entityDef.put(vd.getId(), vd));

    entities.put(entityDef.getId(), entityDef);
    entity.getChildren()
      .forEach(child -> entities.putAll(supplementEntities(child, derivedVariables)));
    return entities;
  }

  @SuppressWarnings("unchecked")
  private S getSpecObject(T request) {
    try {
      Method configGetter = request.getClass().getMethod("getConfig");
      Object config = configGetter.invoke(request);
      if (getConfigurationClass().isAssignableFrom(config.getClass())) {
        return (S)config;
      }
      throw new RuntimeException("Plugin class " + getClass().getName() +
          " declares spec class "  + getConfigurationClass().getName() +
          " but " + request.getClass().getName() + "::getConfig()" +
          " returned " + config.getClass().getName() + ". The second must be a subclass of the first.");
    }
    catch (NoSuchMethodException noSuchMethodException) {
      throw new RuntimeException("Generated class " + request.getClass().getName() +
          " must implement a no-arg method getConfig() which returns an instance of " + getConfigurationClass().getName());
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
      try(AutoCloseableList<InputStream> dataStreams = buildDataStreams()) {
        writeResults(out, dataStreams);
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
      dataStreams.close();
      throw new RuntimeException("Unable to fetch all required data", e);
    }
  }

}
