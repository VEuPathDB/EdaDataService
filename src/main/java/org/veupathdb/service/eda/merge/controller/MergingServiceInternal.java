package org.veupathdb.service.eda.merge.controller;

import jakarta.ws.rs.core.Context;
import org.glassfish.jersey.server.ContainerRequest;
import org.veupathdb.lib.container.jaxrs.server.annotations.Authenticated;
import org.veupathdb.lib.container.jaxrs.server.annotations.DisableJackson;
import org.veupathdb.service.eda.generated.model.DerivedVariableBulkMetadataRequest;
import org.veupathdb.service.eda.generated.model.MergedEntityTabularPostRequest;
import org.veupathdb.service.eda.generated.resources.MergingInternal;

import java.util.Map.Entry;

/**
 * Implementation of endpoints at or below the /merging-internal top-level endpoint.  These endpoints are intended to be
 * accessible only via the internal docker network; therefore although authentication is present, not dataset access
 * restrictions are enforced.  The compute and data service plugins have full access to the datasets so they can
 * process the data down into whatever product they produce.
 */
@Authenticated(allowGuests = true)
public class MergingServiceInternal implements MergingInternal {

  @Context
  ContainerRequest _request;

  @Override
  public PostMergingInternalDerivedVariablesMetadataVariablesResponse postMergingInternalDerivedVariablesMetadataVariables(DerivedVariableBulkMetadataRequest entity) {
    Entry<String,String> authHeader = MergingServiceExternal.getAuthHeader(_request);
    // no need to check perms; only internal clients can access this endpoint
    return PostMergingInternalDerivedVariablesMetadataVariablesResponse.respond200WithApplicationJson(
        MergingServiceExternal.processDvMetadataRequest(entity, authHeader));
  }

  @DisableJackson
  @Override
  public PostMergingInternalQueryResponse postMergingInternalQuery(MergedEntityTabularPostRequest requestBody) {
    Entry<String,String> authHeader = MergingServiceExternal.getAuthHeader(_request);
    // no need to check perms; only internal clients can access this endpoint
    return PostMergingInternalQueryResponse.respond200WithTextTabSeparatedValues(
        MergingServiceExternal.processMergedTabularRequest(requestBody, authHeader));
  }

}
