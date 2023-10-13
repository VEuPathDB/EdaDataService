package org.veupathdb.service.eda.subset.service;

import jakarta.ws.rs.core.Context;
import org.glassfish.jersey.server.ContainerRequest;
import org.veupathdb.lib.container.jaxrs.server.annotations.Authenticated;
import org.veupathdb.lib.container.jaxrs.server.annotations.DisableJackson;
import org.veupathdb.service.eda.data.plugin.filteredmetadata.ContinuousVariablePlugin;
import org.veupathdb.service.eda.generated.model.ContinuousVariableMetadataPostRequest;
import org.veupathdb.service.eda.generated.model.ContinuousVariableMetadataPostResponseStream;
import org.veupathdb.service.eda.generated.resources.FilterAwareMetadataContinuousVariable;

import static org.veupathdb.service.eda.data.AppsService.processRequest;
import static org.veupathdb.service.eda.data.AppsService.wrapPlugin;

@Authenticated(allowGuests = true)
public class FilterAwareMetadataService implements FilterAwareMetadataContinuousVariable {

  @Context
  private ContainerRequest _request;

  @DisableJackson
  @Override
  public PostFilterAwareMetadataContinuousVariableResponse postFilterAwareMetadataContinuousVariable(ContinuousVariableMetadataPostRequest entity) {
    return wrapPlugin(() -> PostFilterAwareMetadataContinuousVariableResponse.respond200WithApplicationJson(
        new ContinuousVariableMetadataPostResponseStream(processRequest(new ContinuousVariablePlugin(), entity, null, _request))));
  }
}
