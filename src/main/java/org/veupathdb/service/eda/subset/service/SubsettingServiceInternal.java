package org.veupathdb.service.eda.subset.service;

import jakarta.ws.rs.core.Context;
import org.glassfish.jersey.server.ContainerRequest;
import org.veupathdb.lib.container.jaxrs.server.annotations.Authenticated;
import org.veupathdb.service.eda.generated.model.EntityCountPostRequest;
import org.veupathdb.service.eda.generated.model.EntityTabularPostRequest;
import org.veupathdb.service.eda.generated.model.VariableDistributionPostRequest;
import org.veupathdb.service.eda.generated.resources.SsInternalStudiesStudyIdEntitiesEntityId;
import org.veupathdb.service.eda.ss.model.tabular.TabularResponses;

/**
 * Provides some of the same endpoints as regular StudiesService but without user authorization checking
 */
@Authenticated(allowGuests = true)
public class SubsettingServiceInternal implements SsInternalStudiesStudyIdEntitiesEntityId {

  @Context
  ContainerRequest _request;

  @Override
  public PostSsInternalStudiesEntitiesCountByStudyIdAndEntityIdResponse
  postSsInternalStudiesEntitiesCountByStudyIdAndEntityId(
      String studyId, String entityId, EntityCountPostRequest entity) {
    return PostSsInternalStudiesEntitiesCountByStudyIdAndEntityIdResponse.respond200WithApplicationJson(
        StudiesService.handleCountRequest(studyId, entityId, entity));
  }

  @Override
  public PostSsInternalStudiesEntitiesTabularByStudyIdAndEntityIdResponse
  postSsInternalStudiesEntitiesTabularByStudyIdAndEntityId(
      String studyId, String entityId, EntityTabularPostRequest requestBody) {
    return StudiesService.handleTabularRequest(_request, studyId, entityId, requestBody, false, (streamer, responseType) ->
      responseType == TabularResponses.Type.JSON
        ? PostSsInternalStudiesEntitiesTabularByStudyIdAndEntityIdResponse
            .respond200WithApplicationJson(streamer)
        : PostSsInternalStudiesEntitiesTabularByStudyIdAndEntityIdResponse
            .respond200WithTextTabSeparatedValues(streamer)
    );
  }

  @Override
  public PostSsInternalStudiesEntitiesVariablesDistributionByStudyIdAndEntityIdAndVariableIdResponse
  postSsInternalStudiesEntitiesVariablesDistributionByStudyIdAndEntityIdAndVariableId(
      String studyId, String entityId, String variableId, VariableDistributionPostRequest entity) {
    return PostSsInternalStudiesEntitiesVariablesDistributionByStudyIdAndEntityIdAndVariableIdResponse.respond200WithApplicationJson(
        StudiesService.handleDistributionRequest(studyId, entityId, variableId, entity));
  }

}
