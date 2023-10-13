package org.veupathdb.service.eda.common.auth;

import jakarta.ws.rs.ForbiddenException;
import org.json.JSONObject;
import org.veupathdb.service.eda.common.client.DatasetAccessClient;

import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Represents a set of permissions a single user has for a single study.  Also provides
 * a static convenience method that can fetch these permissions from a running dataset
 * access service and test them against a predicate.
 */
public class StudyAccess {

  private final boolean _allowStudyMetadata;
  private final boolean _allowSubsetting;
  private final boolean _allowVisualizations;
  private final boolean _allowResultsFirstPage;
  private final boolean _allowResultsAll;

  public StudyAccess(
      final boolean allowStudyMetadata,
      final boolean allowSubsetting,
      final boolean allowVisualizations,
      final boolean allowResultsFirstPage,
      final boolean allowResultsAll
  ) {
    _allowStudyMetadata = allowStudyMetadata;
    _allowSubsetting = allowSubsetting;
    _allowVisualizations = allowVisualizations;
    _allowResultsFirstPage = allowResultsFirstPage;
    _allowResultsAll = allowResultsAll;
  }

  public boolean allowStudyMetadata() { return _allowStudyMetadata; }
  public boolean allowSubsetting() { return _allowSubsetting; }
  public boolean allowVisualizations() { return _allowVisualizations; }
  public boolean allowResultsFirstPage() { return _allowResultsFirstPage; }
  public boolean allowResultsAll() { return _allowResultsAll; }

  /**
   * Looks up study metadata including whether the study is a user study, and
   * permissions for the user represented by the passed header.
   *
   * @param authHeader authentication header to pass to dataset access service
   * @param dataAccessServiceUrl dataset access service URL
   * @param studyId study to look up
   * @param accessPredicate test for access given a permissions map
   */
  public static void confirmPermission(Entry<String,String> authHeader,
      String dataAccessServiceUrl, String studyId, Predicate<StudyAccess> accessPredicate) {
    // check with dataset access service to see if attached auth header has permission to access
    Optional<StudyAccess> perms = new DatasetAccessClient(dataAccessServiceUrl, authHeader).getStudyAccessByStudyId(studyId);
    if (perms.isEmpty() || !accessPredicate.test(perms.get())) {
      throw new ForbiddenException();
    }
  }

  public JSONObject toJson() {
    return new JSONObject()
      .put("allowStudyMetadata", _allowStudyMetadata)
      .put("allowSubsetting", _allowSubsetting)
      .put("allowVisualizations", _allowVisualizations)
      .put("allowResultsFirstPage", _allowResultsFirstPage)
      .put("allowResultsAll", _allowResultsAll);
  }
}
