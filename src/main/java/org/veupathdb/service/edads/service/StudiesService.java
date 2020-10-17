package org.veupathdb.service.edads.service;

import static org.veupathdb.service.edads.service.Utilities.getResponseObject;

import org.veupathdb.service.edads.generated.model.APIStudyDetail;
import org.veupathdb.service.edads.generated.model.EntityIdGetResponse;
import org.veupathdb.service.edads.generated.model.StudiesGetResponse;
import org.veupathdb.service.edads.generated.model.StudyIdGetResponse;
import org.veupathdb.service.edads.generated.resources.Studies;

public class StudiesService implements Studies {

  @Override
  public GetStudiesResponse getStudies() {
    return GetStudiesResponse.respond200WithApplicationJson(
        getResponseObject("/studies", StudiesGetResponse.class));
  }

  @Override
  public GetStudiesByStudyIdResponse getStudiesByStudyId(String studyId) {
    return GetStudiesByStudyIdResponse.respond200WithApplicationJson(getStudyIdGetResponse(studyId));
  }

  public static APIStudyDetail getStudy(String studyId) {
    return getStudyIdGetResponse(studyId).getStudy();
  }

  private static StudyIdGetResponse getStudyIdGetResponse(String studyId) {
    return getResponseObject("/studies/" + studyId, StudyIdGetResponse.class);
  }

  @Override
  public GetStudiesEntitiesByStudyIdAndEntityIdResponse getStudiesEntitiesByStudyIdAndEntityId(String studyId, String entityId) {
    return GetStudiesEntitiesByStudyIdAndEntityIdResponse.respond200WithApplicationJson(
        getResponseObject("/studies/" + studyId + "/entities/" + entityId, EntityIdGetResponse.class));
  }

}
