package org.veupathdb.service.eda.common.client;

import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.core.MediaType;
import org.gusdb.fgputil.client.ClientUtil;
import org.gusdb.fgputil.client.ResponseFuture;
import org.gusdb.fgputil.json.JsonUtil;
import org.gusdb.fgputil.web.MimeTypes;
import org.veupathdb.service.eda.common.client.spec.EdaSubsettingSpecValidator;
import org.veupathdb.service.eda.common.client.spec.StreamSpec;
import org.veupathdb.service.eda.common.client.spec.StreamSpecValidator;
import org.veupathdb.service.eda.common.model.EntityDef;
import org.veupathdb.service.eda.common.model.ReferenceMetadata;
import org.veupathdb.service.eda.common.model.VariableDef;
import org.veupathdb.service.eda.common.model.VariableSource;
import org.veupathdb.service.eda.generated.model.APIFilter;
import org.veupathdb.service.eda.generated.model.APIStudyDetail;
import org.veupathdb.service.eda.generated.model.APIStudyOverview;
import org.veupathdb.service.eda.generated.model.APIVariableDataShape;
import org.veupathdb.service.eda.generated.model.EntityCountPostRequest;
import org.veupathdb.service.eda.generated.model.EntityCountPostRequestImpl;
import org.veupathdb.service.eda.generated.model.EntityCountPostResponse;
import org.veupathdb.service.eda.generated.model.EntityTabularPostRequest;
import org.veupathdb.service.eda.generated.model.EntityTabularPostRequestImpl;
import org.veupathdb.service.eda.generated.model.StudiesGetResponse;
import org.veupathdb.service.eda.generated.model.StudyIdGetResponse;
import org.veupathdb.service.eda.generated.model.ValueSpec;
import org.veupathdb.service.eda.generated.model.VariableDistributionPostRequest;
import org.veupathdb.service.eda.generated.model.VariableDistributionPostRequestImpl;
import org.veupathdb.service.eda.generated.model.VariableDistributionPostResponse;
import org.veupathdb.service.eda.generated.model.VariableSpec;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.gusdb.fgputil.functional.Functions.swallowAndGet;

public class EdaSubsettingClient extends StreamingDataClient {

  // request-scope cache for subsetting service metadata responses
  private List<String> _validStudyNameCache;
  private final Map<String, APIStudyDetail> _studyDetailCache = new HashMap<>();

  public EdaSubsettingClient(String serviceBaseUrl, Entry<String, String> authHeader) {
    super(serviceBaseUrl, authHeader);
  }

  public List<String> getStudies() {
    return _validStudyNameCache != null ? _validStudyNameCache :
      (_validStudyNameCache = swallowAndGet(() -> ClientUtil
          .getResponseObject(getUrl("/studies"), StudiesGetResponse.class, getAuthHeaderMap()))
        .getStudies().stream().map(APIStudyOverview::getId).collect(Collectors.toList()));
  }

  /**
   * Returns the study detail for the study with the passed ID, or an empty optional
   * if no study exists for the passed ID.
   *
   * @param studyId id of a study
   * @return optional study detail for the found study
   */
  public Optional<APIStudyDetail> getStudy(String studyId) {
    if (!getStudies().contains(studyId)) return Optional.empty(); // invalid name
    if (!_studyDetailCache.containsKey(studyId)) {
      _studyDetailCache.put(studyId, swallowAndGet(() -> ClientUtil
          .getResponseObject(getUrl("/studies/" + studyId), StudyIdGetResponse.class, getAuthHeaderMap()).getStudy()));
    }
    return Optional.of(_studyDetailCache.get(studyId));
  }

  @Override
  public StreamSpecValidator getStreamSpecValidator() {
    return new EdaSubsettingSpecValidator();
  }

  @Override
  public String varToColumnHeader(VariableSpec var) {
    return var.getVariableId();
  }

  @Override
  public ResponseFuture getTabularDataStream(
      ReferenceMetadata metadata,
      List<APIFilter> defaultSubset,
      StreamSpec spec) throws ProcessingException {

    // build request object
    EntityTabularPostRequest request = new EntityTabularPostRequestImpl();
    request.setFilters(spec.getFiltersOverride().orElse(defaultSubset));
    request.setOutputVariableIds(spec.stream()
      // subsetting service only takes var IDs (must match entity requested, but should already be validated)
      .map(VariableSpec::getVariableId)
      .collect(Collectors.toList()));

    // build request url using internal endpoint (does not check user permissions via data access service)
    String url = getUrl("/ss-internal/studies/" + metadata.getStudyId() + "/entities/" + spec.getEntityId() + "/tabular");

    // make request
    return ClientUtil.makeAsyncPostRequest(url, request, MimeTypes.TEXT_TABULAR, getAuthHeaderMap());
  }

  public long getSubsetCount(
      ReferenceMetadata metadata,
      String entityId,
      List<APIFilter> subsetFilters
  ) {
    // validate entity ID against this study
    EntityDef entity = metadata.getEntity(entityId).orElseThrow();

    // build request object
    EntityCountPostRequest request = new EntityCountPostRequestImpl();
    request.setFilters(subsetFilters);

    // build request url using internal endpoint (does not check user permissions via data access service)
    String url = getUrl("/ss-internal/studies/" + metadata.getStudyId() + "/entities/" + entity.getId() + "/count");

    // make request
    ResponseFuture response = ClientUtil.makeAsyncPostRequest(url, request, MediaType.APPLICATION_JSON, getAuthHeaderMap());

    // parse output and return
    try (InputStream responseBody = response.getInputStream()) {
      EntityCountPostResponse responseObj = JsonUtil.Jackson.readValue(responseBody, EntityCountPostResponse.class);
      return responseObj.getCount();
    }
    catch (Exception e) {
      throw new RuntimeException("Unable to complete subset count request.", e);
    }
  }

  public VariableDistributionPostResponse getCategoricalDistribution(
      ReferenceMetadata metadata,
      VariableSpec varSpec,
      List<APIFilter> subsetFilters,
      ValueSpec valueSpec
  ) {
    // check variable compatibility with this functionality
    VariableDef var = metadata.getVariable(varSpec).orElseThrow();
    if (var.getSource() != VariableSource.NATIVE) {
      throw new IllegalArgumentException("Cannot call subsetting distribution endpoint with a non-native var: " + var);
    }
    if (var.getDataShape() != APIVariableDataShape.CATEGORICAL) {
      throw new IllegalArgumentException("Cannot call subsetting distribution endpoint with a non-categorical var (for now): " + var);
    }

    // build request object
    VariableDistributionPostRequest request = new VariableDistributionPostRequestImpl();
    request.setFilters(subsetFilters);
    request.setValueSpec(valueSpec);

    // build request url using internal endpoint (does not check user permissions via data access service)
    String url = getUrl("/ss-internal/studies/" + metadata.getStudyId() + "/entities/" + varSpec.getEntityId() + "/variables/" + varSpec.getVariableId() + "/distribution");

    // make request
    ResponseFuture response = ClientUtil.makeAsyncPostRequest(url, request, MediaType.APPLICATION_JSON, getAuthHeaderMap());

    // parse output and return
    try (InputStream responseBody = response.getInputStream()) {
      return JsonUtil.Jackson.readValue(responseBody, VariableDistributionPostResponse.class);
    }
    catch (Exception e) {
      throw new RuntimeException("Unable to complete subset distribution request.", e);
    }
  }
}
