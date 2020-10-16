package org.veupathdb.service.edads.service;

import java.io.IOException;
import java.net.URL;

import org.veupathdb.service.edads.Resources;
import org.veupathdb.service.edads.generated.model.EntityIdGetResponse;
import org.veupathdb.service.edads.generated.model.StudiesGetResponse;
import org.veupathdb.service.edads.generated.model.StudyIdGetResponse;
import org.veupathdb.service.edads.generated.resources.Studies;

import com.fasterxml.jackson.databind.ObjectMapper;

public class StudiesService implements Studies {

  @Override
  public GetStudiesResponse getStudies() {
    return GetStudiesResponse.respond200WithApplicationJson(
        getPassthroughGetResponseObject("/studies", StudiesGetResponse.class));
  }

  @Override
  public GetStudiesByStudyIdResponse getStudiesByStudyId(String studyId) {
    return GetStudiesByStudyIdResponse.respond200WithApplicationJson(
        getPassthroughGetResponseObject("/studies/" + studyId, StudyIdGetResponse.class));
  }

  @Override
  public GetStudiesEntitiesByStudyIdAndEntityIdResponse getStudiesEntitiesByStudyIdAndEntityId(String studyId, String entityId) {
    return GetStudiesEntitiesByStudyIdAndEntityIdResponse.respond200WithApplicationJson(
        getPassthroughGetResponseObject("/studies/" + studyId + "/entities/" + entityId, EntityIdGetResponse.class));
  }

  private <T> T getPassthroughGetResponseObject(String urlPath, Class<T> responseObjectClass) {
    try {
      return new ObjectMapper().readerFor(responseObjectClass).readValue(new URL(Resources.SUBSETTING_SERVICE_URL + urlPath));
    }
    catch (IOException e) {
      throw new RuntimeException("Unable to read and reserialize studies endpoint response object", e);
    }
  }
}
