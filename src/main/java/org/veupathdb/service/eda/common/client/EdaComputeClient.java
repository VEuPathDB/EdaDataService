package org.veupathdb.service.eda.common.client;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.ws.rs.core.MediaType;
import org.gusdb.fgputil.IoUtil;
import org.gusdb.fgputil.client.ClientUtil;
import org.gusdb.fgputil.client.ResponseFuture;
import org.gusdb.fgputil.json.JsonUtil;
import org.veupathdb.service.eda.generated.model.APIFilter;
import org.veupathdb.service.eda.generated.model.ComputeRequestBaseImpl;
import org.veupathdb.service.eda.generated.model.ComputedVariableMetadata;
import org.veupathdb.service.eda.generated.model.ComputedVariableMetadataImpl;
import org.veupathdb.service.eda.generated.model.DerivedVariableSpec;
import org.veupathdb.service.eda.generated.model.JobResponse;
import org.veupathdb.service.eda.generated.model.JobStatus;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class EdaComputeClient {

  public static class ComputeRequestBody extends ComputeRequestBaseImpl {

    @JsonIgnore
    private final Object _computeConfig;

    public ComputeRequestBody(
        String studyId, List<APIFilter> subset, List<DerivedVariableSpec> derivedVariables, Object computeConfig) {
      setStudyId(studyId);
      setFilters(subset);
      setDerivedVariables(derivedVariables);
      _computeConfig = computeConfig;
    }

    @JsonProperty("config")
    public Object getConfig() {
      return _computeConfig;
    }
  }

  // parent path returns status; flag indicates not to restart
  private static final String STATUS_SEGMENT = "?autostart=false";

  private static final String META_FILE_SEGMENT = "/meta";
  private static final String STATS_FILE_SEGMENT = "/statistics";
  private static final String TABULAR_FILE_SEGMENT = "/tabular";

  private final String _baseComputesUrl;
  private final Map<String, String> _authHeader;

  public EdaComputeClient(String serviceBaseUrl, Entry<String, String> authHeader) {
    _baseComputesUrl = serviceBaseUrl + "/computes/";
    _authHeader = Map.of(authHeader.getKey(), authHeader.getValue());
  }

  public boolean isJobResultsAvailable(String computeName, ComputeRequestBody requestBody) {
    JobResponse response = readJsonResponse(getResponseFuture(computeName, STATUS_SEGMENT, requestBody), JobResponse.class);
    return response.getStatus().equals(JobStatus.COMPLETE);
  }

  public ComputedVariableMetadata getJobVariableMetadata(String computeName, ComputeRequestBody requestBody) {
    return readJsonResponse(getResponseFuture(computeName, META_FILE_SEGMENT, requestBody), ComputedVariableMetadataImpl.class);
  }

  public ResponseFuture getJobStatistics(String computeName, ComputeRequestBody requestBody) {
    return getResponseFuture(computeName, STATS_FILE_SEGMENT, requestBody);
  }

  public <T> T getJobStatistics(String computeName, ComputeRequestBody requestBody, Class<T> expectedStatsClass) {
    return readJsonResponse(getResponseFuture(computeName, STATS_FILE_SEGMENT, requestBody), expectedStatsClass);
  }

  public ResponseFuture getJobTabularOutput(String computeName, ComputeRequestBody requestBody) {
    return getResponseFuture(computeName, TABULAR_FILE_SEGMENT, requestBody);
  }

  private ResponseFuture getResponseFuture(String computeName, String fileSegment, ComputeRequestBody requestBody) {
    return ClientUtil.makeAsyncPostRequest(
        // note: need to use wildcard here since compute service serves all result files out at the same endpoint
        _baseComputesUrl + computeName + fileSegment, requestBody, MediaType.MEDIA_TYPE_WILDCARD, _authHeader);
  }

  private <T> T readJsonResponse(ResponseFuture response, Class<T> responseClass) {
    try (InputStream responseBody = response.getEither().leftOrElseThrowWithRight(f -> new RuntimeException(f.toString()))) {
      String json = IoUtil.readAllChars(new InputStreamReader(responseBody));
      return JsonUtil.Jackson.readValue(json, responseClass);
    }
    catch (Exception e) {
      throw new RuntimeException("Unable to make compute request or read/convert response", e);
    }
  }
}
