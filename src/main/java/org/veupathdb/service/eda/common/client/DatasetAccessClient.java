package org.veupathdb.service.eda.common.client;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.gusdb.fgputil.client.ClientUtil;
import org.gusdb.fgputil.client.RequestFailure;
import org.gusdb.fgputil.functional.Either;
import org.gusdb.fgputil.runtime.Environment;
import org.json.JSONObject;
import org.veupathdb.service.eda.access.service.permissions.PermissionService;
import org.veupathdb.service.eda.common.auth.StudyAccess;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

public class DatasetAccessClient extends ServiceClient {

  private static final String ENABLE_DATASET_ACCESS_RESTRICTIONS = "ENABLE_DATASET_ACCESS_RESTRICTIONS";

  public static class BasicStudyDatasetInfo {

    private final String _studyId;
    private final String _datasetId;
    private final boolean _isUserStudy;
    private final StudyAccess _studyAccess;

    public BasicStudyDatasetInfo(JSONObject json) {
      this(json.getString("datasetId"), json);
    }

    public BasicStudyDatasetInfo(String datasetId, JSONObject json) {
      _datasetId = datasetId;
      _studyId = json.getString("studyId");
      _isUserStudy = json.getBoolean("isUserStudy");
      JSONObject studyPerms = json.getJSONObject("actionAuthorization");
      _studyAccess = new StudyAccess(
          studyPerms.getBoolean("studyMetadata"),
          studyPerms.getBoolean("subsetting"),
          studyPerms.getBoolean("visualizations"),
          studyPerms.getBoolean("resultsFirstPage"),
          studyPerms.getBoolean("resultsAll")
      );
    }

    public String getStudyId() { return _studyId; }
    public String getDatasetId() { return _datasetId; }
    public boolean isUserStudy() { return _isUserStudy; }
    public StudyAccess getStudyAccess() { return _studyAccess; }

    @Override
    public String toString() {
      return toJson().toString();
    }

    public JSONObject toJson() {
      return new JSONObject()
          .put("studyId", _studyId)
          .put("datasetId", _datasetId)
          .put("isUserStudy", _isUserStudy)
          .put("studyAccess", _studyAccess.toJson());
    }
  }

  public static class StudyDatasetInfo extends BasicStudyDatasetInfo {

    private final String _sha1Hash;
    private final String _displayName;
    private final String _shortDisplayName;
    private final String _description;

    public StudyDatasetInfo(String datasetId, JSONObject json) {
      super(datasetId, json);
      _sha1Hash = json.optString("sha1Hash", "");
      _displayName = json.getString("displayName");
      _shortDisplayName = json.optString("shortDisplayName", _displayName);
      _description = json.optString("description", null);
    }

    public String getSha1Hash() { return _sha1Hash; }
    public String getDisplayName() { return _displayName; }
    public String getShortDisplayName() { return _shortDisplayName; }
    public String getDescription() { return _description; }

    public JSONObject toJson() {
      return super.toJson()
          .put("sha1Hash", _sha1Hash)
          .put("displayName", _displayName)
          .put("shortDisplayName", _shortDisplayName)
          .put("description", _description);
    }
  }

  public DatasetAccessClient(String baseUrl, Entry<String,String> authHeader) {
    super(baseUrl, authHeader);
  }

  /**
   * Builds a map from study ID to study info, including permissions for each curated
   * study, plus user studies this user has access to.  Requires a request to the dataset access service.
   *
   * @return study map with study IDs as keys
   */
  public Map<String, StudyDatasetInfo> getStudyDatasetInfoMapForUser() {
    try (InputStream response = ClientUtil.makeAsyncGetRequest(getUrl("/permissions"),
        MediaType.APPLICATION_JSON, getAuthHeaderMap()).getInputStream()) {
      String permissionsJson = ClientUtil.readSmallResponseBody(response);
      JSONObject datasetMap = new JSONObject(permissionsJson).getJSONObject("perDataset");
      Map<String, StudyDatasetInfo> infoMap = new HashMap<>();
      for (String datasetId : datasetMap.keySet()) {
        JSONObject datasetInfoJson = datasetMap.getJSONObject(datasetId);
        StudyDatasetInfo datasetInfo = new StudyDatasetInfo(datasetId, datasetInfoJson);
        // reorganizing here; response JSON is keyed by dataset ID, but resulting map is keyed by study ID
        infoMap.put(datasetInfo.getStudyId(), datasetInfo);
      }
      return infoMap;
    }
    catch (Exception e) {
      throw new RuntimeException("Unable to read permissions response", e);
    }
  }

  /**
   * Looks up permissions for a user on a particular dataset; however, unlike
   * <code>getStudyDatasetInfoMapForUser()</code>, this method will look up user
   * studies the user does NOT have permissions on, and still return information
   * about the study (with false for all perms).  A passed datasetId string that
   * does not exist (curated or user study, regardless of this user's perms)
   * will result in a NotFoundException.
   *
   * @param datasetId dataset ID for study to look up
   * @return dataset access service metadata about this study
   */
  public BasicStudyDatasetInfo getStudyPermsByDatasetId(String datasetId) {
    try {
      Either<InputStream, RequestFailure> response = ClientUtil
          .makeAsyncGetRequest(getUrl("/permissions/" + datasetId),
              MediaType.APPLICATION_JSON, getAuthHeaderMap()).getEither();
      response.ifRight(fail -> {
        // check for 404
        if (fail.getStatusType().getStatusCode() == Response.Status.NOT_FOUND.getStatusCode()) {
          throw new NotFoundException("Dataset Access: no study found with dataset ID " + datasetId);
        }
        throw new RuntimeException("Failed to request permissions from dataset access: " + fail.toString());
      });
      try (InputStream responseBody = response.getLeft()) {
        JSONObject json = new JSONObject(ClientUtil.readSmallResponseBody(responseBody));
        return new BasicStudyDatasetInfo(json);
      }
    }
    catch (WebApplicationException e) {
      throw e;
    }
    catch (Exception e) {
      throw new RuntimeException("Unable to read permissions response", e);
    }
  }

  /**
   * Looks up the permissions for the authenticated user, finds the study for the passed study ID, and
   * returns only the StudyAccess portion of the dataset found.  This calls getStudyDatasetInfoMapForUser()
   * so will return an empty optional unless the study is a curated study or the user has access via
   * shared user dataset (even if the study exists as a user study this user does not have permissions on).
   *
   * Note this method (but not others) respects the ENABLE_DATASET_ACCESS_RESTRICTIONS environment variable;
   * if set to false, the dataset access service is NOT queried, and a StudyAccess object is returned
   * granting universal access to the study.  This was a hack added during development to support DBs not
   * entirely populated with data and should eventually be removed.
   *
   * @param studyId study for which perms should be looked up
   * @return set of access permissions to this study
   */
  public Optional<StudyAccess> getStudyAccessByStudyId(String studyId) {
    if (!Boolean.parseBoolean(Environment.getOptionalVar(ENABLE_DATASET_ACCESS_RESTRICTIONS, Boolean.TRUE.toString()))) {
      return Optional.of(new StudyAccess(true, true, true, true, true));
    }
    // get the perms for this user of known studies
    return getStudyDatasetInfoMapForUser().values().stream()
        // filter to find this study
        .filter(info -> info.getStudyId().equals(studyId))
        // convert to optional
        .findAny()
        // fish out the perms
        .map(BasicStudyDatasetInfo::getStudyAccess);
  }

}
