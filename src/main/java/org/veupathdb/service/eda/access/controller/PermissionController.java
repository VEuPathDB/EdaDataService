package org.veupathdb.service.eda.access.controller;

import jakarta.ws.rs.core.Context;
import org.glassfish.jersey.server.ContainerRequest;
import org.veupathdb.lib.container.jaxrs.server.annotations.Authenticated;
import org.veupathdb.service.eda.generated.resources.Permissions;
import org.veupathdb.service.eda.access.service.permissions.PermissionService;

@Authenticated(allowGuests = true)
public class PermissionController implements Permissions
{
  @Context
  private ContainerRequest _request;

  @Override
  public GetPermissionsResponse getPermissions() {
    return GetPermissionsResponse.respond200WithApplicationJson(
      PermissionService.getInstance().getUserPermissions(_request)
    );
  }

  @Override
  public GetPermissionsByDatasetIdResponse getPermissionsByDatasetId(String datasetId) {
    return GetPermissionsByDatasetIdResponse.respond200WithApplicationJson(
        PermissionService.getInstance().getUserPermissions(_request, datasetId));
  }
}
