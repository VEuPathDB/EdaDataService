package org.veupathdb.service.eda.subset.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.validation.ValidationException;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.server.ContainerRequest;
import org.gusdb.fgputil.ListBuilder;
import org.gusdb.fgputil.MapBuilder;
import org.gusdb.fgputil.distribution.DistributionResult;
import org.gusdb.fgputil.functional.TreeNode;
import org.gusdb.fgputil.json.JsonUtil;
import org.gusdb.fgputil.web.UrlEncodedForm;
import org.veupathdb.lib.container.jaxrs.model.User;
import org.veupathdb.lib.container.jaxrs.providers.UserProvider;
import org.veupathdb.lib.container.jaxrs.server.annotations.Authenticated;
import org.veupathdb.lib.container.jaxrs.server.middleware.CustomResponseHeadersFilter;
import org.veupathdb.service.eda.Resources;
import org.veupathdb.service.eda.common.auth.StudyAccess;
import org.veupathdb.service.eda.common.client.DatasetAccessClient;
import org.veupathdb.service.eda.common.client.DatasetAccessClient.StudyDatasetInfo;
import org.veupathdb.service.eda.generated.model.APIEntity;
import org.veupathdb.service.eda.generated.model.APIStudyDetail;
import org.veupathdb.service.eda.generated.model.EntityCountPostRequest;
import org.veupathdb.service.eda.generated.model.EntityCountPostResponse;
import org.veupathdb.service.eda.generated.model.EntityCountPostResponseImpl;
import org.veupathdb.service.eda.generated.model.EntityIdGetResponse;
import org.veupathdb.service.eda.generated.model.EntityIdGetResponseImpl;
import org.veupathdb.service.eda.generated.model.EntityTabularPostRequest;
import org.veupathdb.service.eda.generated.model.EntityTabularPostResponseStream;
import org.veupathdb.service.eda.generated.model.StudiesGetResponse;
import org.veupathdb.service.eda.generated.model.StudiesGetResponseImpl;
import org.veupathdb.service.eda.generated.model.StudyIdGetResponse;
import org.veupathdb.service.eda.generated.model.StudyIdGetResponseImpl;
import org.veupathdb.service.eda.generated.model.ValueSpec;
import org.veupathdb.service.eda.generated.model.VariableDistributionPostRequest;
import org.veupathdb.service.eda.generated.model.VariableDistributionPostResponse;
import org.veupathdb.service.eda.generated.model.VariableDistributionPostResponseImpl;
import org.veupathdb.service.eda.generated.model.VocabByRootEntityPostRequest;
import org.veupathdb.service.eda.generated.model.VocabByRootEntityPostResponseStream;
import org.veupathdb.service.eda.generated.resources.Studies;
import org.veupathdb.service.eda.ss.model.Entity;
import org.veupathdb.service.eda.ss.model.Study;
import org.veupathdb.service.eda.ss.model.StudyOverview;
import org.veupathdb.service.eda.ss.model.db.*;
import org.veupathdb.service.eda.ss.model.distribution.DistributionFactory;
import org.veupathdb.service.eda.ss.model.reducer.BinaryValuesStreamer;
import org.veupathdb.service.eda.ss.model.reducer.MetadataFileBinaryProvider;
import org.veupathdb.service.eda.ss.model.tabular.DataSourceType;
import org.veupathdb.service.eda.ss.model.tabular.TabularReportConfig;
import org.veupathdb.service.eda.ss.model.tabular.TabularResponses;
import org.veupathdb.service.eda.ss.model.variable.Variable;
import org.veupathdb.service.eda.ss.model.variable.VariableType;
import org.veupathdb.service.eda.ss.model.variable.VariableWithValues;
import org.veupathdb.service.eda.ss.model.variable.binary.BinaryFilesManager;
import org.veupathdb.service.eda.ss.model.variable.binary.SimpleStudyFinder;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.veupathdb.service.eda.subset.service.ApiConversionUtil.*;

@Authenticated(allowGuests = true)
public class StudiesService implements Studies {
  private static final Logger LOG = LogManager.getLogger(StudiesService.class);

  private static final long MAX_ROWS_FOR_SINGLE_PAGE_ACCESS = 20;

  @Context
  ContainerRequest _request;

  @Override
  public GetStudiesResponse getStudies() {
    // get IDs of studies visible to this user
    Map<String, StudyDatasetInfo> visibleStudyMap = new DatasetAccessClient(
        Resources.getDatasetAccessServiceUrl(),
        UserProvider.getSubmittedAuth(_request).orElseThrow()
    ).getStudyDatasetInfoMapForUser();
    Set<String> visibleStudyIds = visibleStudyMap.keySet();

    // filter overviews by visible studies
    Map<String, StudyOverview> visibleOverviewMap = getStudyResolver()
        .getStudyOverviews().stream()
        .filter(overview -> visibleStudyIds.contains(overview.getStudyId()))
        .collect(Collectors.toMap(StudyOverview::getStudyId, Function.identity()));

    // convert to API objects and return
    StudiesGetResponse out = new StudiesGetResponseImpl();
    out.setStudies(ApiConversionUtil.toApiStudyOverviews(visibleStudyMap, visibleOverviewMap));
    return GetStudiesResponse.respond200WithApplicationJson(out);
  }

  @Override
  public GetStudiesByStudyIdResponse getStudiesByStudyId(String studyId) {
    checkPerms(_request, studyId, StudyAccess::allowStudyMetadata);
    Study study = getStudyResolver().getStudyById(studyId);
    APIStudyDetail apiStudyDetail = ApiConversionUtil.getApiStudyDetail(study);
    StudyIdGetResponse response = new StudyIdGetResponseImpl();
    response.setStudy(apiStudyDetail);
    return GetStudiesByStudyIdResponse.respond200WithApplicationJson(response);
  }

  @Override
  public GetStudiesEntitiesByStudyIdAndEntityIdResponse getStudiesEntitiesByStudyIdAndEntityId(String studyId, String entityId) {
    checkPerms(_request, studyId, StudyAccess::allowStudyMetadata);
    APIStudyDetail apiStudyDetail = ApiConversionUtil.getApiStudyDetail(getStudyResolver().getStudyById(studyId));
    APIEntity entity = findEntityById(apiStudyDetail.getRootEntity(), entityId).orElseThrow(NotFoundException::new);
    EntityIdGetResponse response = new EntityIdGetResponseImpl();
    // copy properties of found entity, skipping children
    response.setId(entity.getId());
    response.setDescription(entity.getDescription());
    response.setDisplayName(entity.getDisplayName());
    response.setDisplayNamePlural(entity.getDisplayNamePlural());
    response.setVariables(entity.getVariables());
    response.setCollections(entity.getCollections());
    for (Entry<String,Object> prop : entity.getAdditionalProperties().entrySet()) {
      response.setAdditionalProperties(prop.getKey(), prop.getValue());
    }
    return GetStudiesEntitiesByStudyIdAndEntityIdResponse.respond200WithApplicationJson(response);
  }

  @Override
  public PostStudiesEntitiesVariablesDistributionByStudyIdAndEntityIdAndVariableIdResponse 
  postStudiesEntitiesVariablesDistributionByStudyIdAndEntityIdAndVariableId(
      String studyId, String entityId, String variableId, VariableDistributionPostRequest request) {
    checkPerms(_request, studyId, StudyAccess::allowSubsetting);
    return PostStudiesEntitiesVariablesDistributionByStudyIdAndEntityIdAndVariableIdResponse.
        respond200WithApplicationJson(handleDistributionRequest(studyId, entityId, variableId, request));
  }



  public static VariableDistributionPostResponse handleDistributionRequest(
      String studyId, String entityId, String variableId, VariableDistributionPostRequest request) {
    try {
      Study study = getStudyResolver().getStudyById(studyId);
      String dataSchema = resolveSchema(study);

      // unpack data from API input to model objects
      RequestBundle req = RequestBundle.unpack(dataSchema, study, entityId, request.getFilters(), ListBuilder.asList(variableId), null);

      // FIXME: need this until we turn on schema-level checking to enforce requiredness
      if (request.getValueSpec() == null) request.setValueSpec(ValueSpec.COUNT);

      DistributionResult result = DistributionFactory.processDistributionRequest(
          Resources.getApplicationDataSource(), dataSchema, req.getStudy(), req.getTargetEntity(),
          getRequestedVariable(req), req.getFilters(), toInternalValueSpec(request.getValueSpec()),
          toInternalBinSpecWithRange(request.getBinSpec()));

      VariableDistributionPostResponse response = new VariableDistributionPostResponseImpl();
      response.setHistogram(toApiHistogramBins(result.getHistogramData()));
      response.setStatistics(toApiHistogramStats(result.getStatistics()));
      return response;
    }
    catch (RuntimeException e) {
      LOG.error("Unable to deliver distribution response", e);
      throw e;
    }
  }

  @Override
  public PostStudiesEntitiesTabularByStudyIdAndEntityIdResponse postStudiesEntitiesTabularByStudyIdAndEntityId(String studyId,
      String entityId, EntityTabularPostRequest requestBody) {
    return handleTabularRequest(_request, studyId, entityId, requestBody, true, (streamer, responseType) ->
      responseType == TabularResponses.Type.JSON
        ? PostStudiesEntitiesTabularByStudyIdAndEntityIdResponse
            .respond200WithApplicationJson(streamer)
        : PostStudiesEntitiesTabularByStudyIdAndEntityIdResponse
            .respond200WithTextTabSeparatedValues(streamer)
    );
  }

  @Override
  public PostStudiesEntitiesTabularByStudyIdAndEntityIdResponse postStudiesEntitiesTabularByStudyIdAndEntityId(String studyId, String entityId) {
    UrlEncodedForm form = new UrlEncodedForm(_request.getEntityStream());
    String requestJson = form.getFirstParamValue("data")
        .orElseThrow(() -> new BadRequestException(
            "Form must contain parameter 'data' containing tabular request JSON."));
    try {
      EntityTabularPostRequest request = JsonUtil.Jackson.readValue(requestJson, EntityTabularPostRequest.class);
      PostStudiesEntitiesTabularByStudyIdAndEntityIdResponse typedResponse =
          postStudiesEntitiesTabularByStudyIdAndEntityId(studyId, entityId, request);
      // success so far; add header to response
      String entityDisplay = getStudyResolver().getStudyById(studyId).getEntity(entityId).orElseThrow().getDisplayName();
      String fileName = studyId + "_" + entityDisplay + "_subsettedData.txt";
      String dispositionHeaderValue = "attachment; filename=\"" + fileName + "\"";
      ServiceMetrics.reportSubsetDownload(studyId, UserProvider.lookupUser(_request)
          .map(User::getUserID)
          .map(id -> Long.toString(id))
          .orElse("None"), entityDisplay);
      _request.setProperty(CustomResponseHeadersFilter.CUSTOM_HEADERS_KEY,
          new MapBuilder<>(HttpHeaders.CONTENT_DISPOSITION, dispositionHeaderValue).toMap());
      return typedResponse;
    }
    catch (JsonProcessingException e) {
      throw new BadRequestException(e.getMessage());
    }
  }

  @Override
  public PostStudiesEntitiesVariablesRootVocabByStudyIdAndEntityIdAndVariableIdResponse postStudiesEntitiesVariablesRootVocabByStudyIdAndEntityIdAndVariableId(String studyId, String entityId, String variableId, VocabByRootEntityPostRequest body) {
    checkPerms(_request, studyId, StudyAccess::allowSubsetting);
    Study study = getStudyResolver().getStudyById(studyId);
    String dataSchema = resolveSchema(study);


    // Validate entity/variable ID existence
    Variable var = study.getEntity(entityId)
        .orElseThrow(() -> new ValidationException(String.format("Entity %s not found in study %s.", entityId, studyId)))
        .getVariable(variableId)
        .orElseThrow(() -> new ValidationException(String.format("Variable ID %s not found on entity %s in study %s.", variableId, entityId, studyId)));

    // Trivially, only works on variables with values
    if (!(var instanceof VariableWithValues<?>)) {
      throw new ValidationException("Unable to retrieve vocabulary for a variable without values.");
    }

    VariableWithValues<?> variableWithValues = (VariableWithValues<?>) var;
    if (variableWithValues.getType() != VariableType.STRING || variableWithValues.getVocabulary() == null) {
      throw new ValidationException("Specified variable must be a string with a vocabulary.");
    }

    VocabByRootEntityPostResponseStream streamer = new VocabByRootEntityPostResponseStream(outputStream -> {
      final OutputStreamWriter writer = new OutputStreamWriter(outputStream);
      final BufferedWriter bufferedWriter = new BufferedWriter(writer);
      TabularResponses.ResultConsumer resultConsumer = TabularResponses.Type.TABULAR.getFormatter().getFormatter(bufferedWriter);

      // TODO: probably want a singleton RootVocabHandler with caching implemented.
      final RootVocabHandler vocabHandler = new RootVocabHandler();
      vocabHandler.queryStudyVocab(dataSchema,
          Resources.getApplicationDataSource(),
          study.getEntityTree().getContents(),
          variableWithValues,
          resultConsumer,
          toInternalFilters(study, body.getFilters(), dataSchema));

      try {
        bufferedWriter.flush();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    });

    return PostStudiesEntitiesVariablesRootVocabByStudyIdAndEntityIdAndVariableIdResponse.respond200WithTextTabSeparatedValues(streamer);
  }

  public static <T> T handleTabularRequest(
      ContainerRequest requestContext, String studyId, String entityId,
      EntityTabularPostRequest requestBody, boolean checkUserPermissions,
      BiFunction<EntityTabularPostResponseStream,TabularResponses.Type,T> responseConverter) {
    LOG.info("Handling tabular request for study {} and entity {}.", studyId, entityId);
    Study study = getStudyResolver().getStudyById(studyId);
    String dataSchema = resolveSchema(study);
    RequestBundle request = RequestBundle.unpack(dataSchema, study, entityId, requestBody.getFilters(), requestBody.getOutputVariableIds(), requestBody.getReportConfig());

    if (checkUserPermissions) {
      // if requested, make sure user has permission to access this amount of tabular data (may differ based on report config)
      checkPerms(requestContext, studyId, getTabularAccessPredicate(request.getReportConfig()));
    }

    // Oracle tables do not support >1000 columns; if an entity has a total of >1000 columns then the "wide table"
    //   of data is not generated for that entity ("tall table" still is).  This only affects paged/sorted tabular
    //   results, so add a check here for that kind of request on that kind of entity and throw 400 in that case.
    Entity entity = request.getTargetEntity();
    if (request.getReportConfig().requiresSorting() &&
        // total columns equals IDs for the entity and ancestors + each variable (not sure if category/collection vars should count, but this is safe)
        1 + entity.getAncestorPkColNames().size() + entity.getVariables().size() > 1000) {
      throw new BadRequestException("Tabular requests with paging/sorting are not supported on entities with >1000 total columns");
    }

    TabularResponses.Type responseType = TabularResponses.Type.fromAcceptHeader(requestContext);
    final BinaryFilesManager binaryFilesManager = Resources.getBinaryFilesManager();
    final BinaryValuesStreamer binaryValuesStreamer = new BinaryValuesStreamer(binaryFilesManager,
            Resources.getFileChannelThreadPool(), Resources.getDeserializerThreadPool());
    if (shouldRunFileBasedSubsetting(request, binaryFilesManager)) {
      LOG.info("Running file-based subsetting for study " + studyId);
      EntityTabularPostResponseStream streamer = new EntityTabularPostResponseStream(outStream ->
          FilteredResultFactory.produceTabularSubsetFromFile(request.getStudy(), entity,
              request.getRequestedVariables(), request.getFilters(), responseType.getBinaryFormatter(),
              request.getReportConfig(), outStream, binaryValuesStreamer));
      return responseConverter.apply(streamer, responseType);
    }
    LOG.info("Performing oracle-based subsetting for study " + studyId);
    EntityTabularPostResponseStream streamer = new EntityTabularPostResponseStream(outStream ->
        FilteredResultFactory.produceTabularSubset(Resources.getApplicationDataSource(), dataSchema,
            request.getStudy(), entity, request.getRequestedVariables(), request.getFilters(),
            request.getReportConfig(), responseType.getFormatter(), outStream));
    return responseConverter.apply(streamer, responseType);
  }

  private static Predicate<StudyAccess> getTabularAccessPredicate(TabularReportConfig reportConfig) {
    // trigger single-page access IFF user specifies paging with zero offset and number of rows under the single-page max
    if (reportConfig.getOffset() == 0L &&
        reportConfig.getNumRows().isPresent() &&
        reportConfig.getNumRows().get() <= MAX_ROWS_FOR_SINGLE_PAGE_ACCESS) {
      return StudyAccess::allowResultsFirstPage;
    }
    // if paging not present or does not meet single-page criteria, user needs all-results access
    return StudyAccess::allowResultsAll;
  }

  /**
   * Skip and do oracle-based subsetting if either:
   * 1. File-based subsetting is disabled via environment variable
   * 2. getReportConfig().getDataSourceType() is not specified or is specified as DATABASE
   * 3. Any data is missing in files (i.e. MissingDataException is thrown).
   **/
  private static boolean shouldRunFileBasedSubsetting(RequestBundle requestBundle, BinaryFilesManager binaryFilesManager) {
    if (!Resources.isFileBasedSubsettingEnabled() || requestBundle.getReportConfig().getDataSourceType() == DataSourceType.DATABASE) {
      return false;
    }
    if (!binaryFilesManager.studyHasFiles(requestBundle.getStudy())) {
      LOG.info("Unable to find study dir for " + requestBundle.getStudy().getStudyId() + " in study files.");
      return false;
    }
    if (!binaryFilesManager.entityDirExists(requestBundle.getStudy(), requestBundle.getTargetEntity())) {
      LOG.info("Unable to find entity dir for " + requestBundle.getTargetEntity().getId() + " in study files.");
      return false;
    }
    if (!binaryFilesManager.idMapFileExists(requestBundle.getStudy(), requestBundle.getTargetEntity())) {
      LOG.info("Unable to find ID file for " + requestBundle.getTargetEntity().getId() + " in study files.");
      return false;
    }
    if (!requestBundle.getTargetEntity().getAncestorEntities().isEmpty() && !binaryFilesManager.ancestorFileExists(requestBundle.getStudy(), requestBundle.getTargetEntity())) {
      LOG.info("Unable to find ancestor file for " + requestBundle.getTargetEntity().getId() + " in study files.");
      return false;
    }
    for (VariableWithValues outputVar: requestBundle.getRequestedVariables()) {
      if (!binaryFilesManager.variableFileExists(requestBundle.getStudy(), requestBundle.getTargetEntity(), outputVar)) {
        LOG.info("Unable to find output var " + outputVar.getId() + " in study files.");
        return false;
      }
    }
    List<VariableWithValues> filterVars = requestBundle.getFilters().stream()
        .flatMap(filter -> filter.getAllVariables().stream())
        .collect(Collectors.toList());
    for (VariableWithValues filterVar: filterVars) {
      if (!binaryFilesManager.variableFileExists(requestBundle.getStudy(), filterVar.getEntity(), filterVar)) {
        LOG.info("Unable to find filterVar var " + filterVar.getId() + " in study files.");
        return false;
      }
    }
    return true;
  }

  @Override
  public PostStudiesEntitiesCountByStudyIdAndEntityIdResponse postStudiesEntitiesCountByStudyIdAndEntityId(
      String studyId, String entityId, EntityCountPostRequest rawRequest) {
    checkPerms(_request, studyId, StudyAccess::allowSubsetting);
    return PostStudiesEntitiesCountByStudyIdAndEntityIdResponse.respond200WithApplicationJson(
        handleCountRequest(studyId, entityId, rawRequest));
  }

  public static EntityCountPostResponse handleCountRequest(String studyId, String entityId, EntityCountPostRequest rawRequest) {
    LOG.info("Handling count request.");
    Study study = getStudyResolver().getStudyById(studyId);
    String dataSchema = resolveSchema(study);

    // unpack data from API input to model objects
    RequestBundle request = RequestBundle.unpack(dataSchema, study, entityId, rawRequest.getFilters(), Collections.emptyList(), null);

    TreeNode<Entity> prunedEntityTree = FilteredResultFactory.pruneTree(
        request.getStudy().getEntityTree(), request.getFilters(), request.getTargetEntity());

    final BinaryFilesManager binaryFilesManager = new BinaryFilesManager(
        new SimpleStudyFinder(Resources.getBinaryFilesDirectory().toString()));

    final BinaryValuesStreamer binaryValuesStreamer = new BinaryValuesStreamer(binaryFilesManager,
            Resources.getFileChannelThreadPool(), Resources.getDeserializerThreadPool());

    EntityCountPostResponse response = new EntityCountPostResponseImpl();
    if (shouldRunFileBasedSubsetting(request, binaryFilesManager)) {
      long count = FilteredResultFactory.getEntityCount(prunedEntityTree, request.getTargetEntity(), request.getFilters(),
              binaryValuesStreamer, study);
      response.setCount(count);
    }
    else {
      long count = FilteredResultFactory.getEntityCount(
          Resources.getApplicationDataSource(), dataSchema, prunedEntityTree, request.getTargetEntity(), request.getFilters());
      response.setCount(count);
    }

    return response;
  }

  private static void checkPerms(ContainerRequest request, String studyId, Predicate<StudyAccess> accessPredicate) {
    Entry<String, String> authHeader = UserProvider.getSubmittedAuth(request).orElseThrow();
    StudyAccess.confirmPermission(authHeader, Resources.getDatasetAccessServiceUrl(), studyId, accessPredicate);
  }


  public static StudyProvider getStudyResolver() {
    final BinaryFilesManager binaryFilesManager = Resources.getBinaryFilesManager();
    final MetadataFileBinaryProvider metadataFileBinaryProvider = new MetadataFileBinaryProvider(binaryFilesManager);
    final VariableFactory variableFactory = new VariableFactory(Resources.getApplicationDataSource(),
        Resources.getUserStudySchema(),
        metadataFileBinaryProvider,
        binaryFilesManager::studyHasFiles);
    return new StudyResolver(
        Resources.getMetadataCache(),
        new StudyFactory(
            Resources.getApplicationDataSource(),
            Resources.getUserStudySchema(),
            StudyOverview.StudySourceType.USER_SUBMITTED,
            variableFactory)
    );
  }

  private static String resolveSchema(Study study) {
    return switch(study.getStudySourceType()) {
      case USER_SUBMITTED -> Resources.getUserStudySchema();
      case CURATED -> Resources.getAppDbSchema();
    };
  }

  private Optional<APIEntity> findEntityById(APIEntity entity, String entityId) {
    if (entity.getId().equals(entityId)) {
      return Optional.of(entity);
    }
    for (APIEntity child : entity.getChildren()){
      Optional<APIEntity> foundEntity = findEntityById(child, entityId);
      if (foundEntity.isPresent()) return foundEntity;
    }
    return Optional.empty();
  }

  private static VariableWithValues<?> getRequestedVariable(RequestBundle req) {
    if (req.getRequestedVariables().isEmpty()) {
      throw new RuntimeException("No requested variables (empty URL segment?)");
    }
    Variable var = req.getRequestedVariables().get(0);
    if (!(var instanceof VariableWithValues)) {
      throw new BadRequestException("Distribution endpoint can only be called with a variable that has values.");
    }
    return (VariableWithValues<?>)var;
  }
}
