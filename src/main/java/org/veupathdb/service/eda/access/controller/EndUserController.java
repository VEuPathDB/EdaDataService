package org.veupathdb.service.eda.access.controller;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Context;
import org.glassfish.jersey.server.ContainerRequest;
import org.veupathdb.lib.container.jaxrs.server.annotations.Authenticated;
import org.veupathdb.service.eda.generated.model.ApprovalStatus;
import org.veupathdb.service.eda.generated.model.EndUserCreateRequest;
import org.veupathdb.service.eda.generated.model.EndUserPatch;
import org.veupathdb.service.eda.generated.resources.DatasetEndUsers;
import org.veupathdb.service.eda.access.service.provider.ProviderService;
import org.veupathdb.service.eda.access.service.user.EndUserCreationService;
import org.veupathdb.service.eda.access.service.user.EndUserDeleteService;
import org.veupathdb.service.eda.access.service.user.EndUserLookupService;
import org.veupathdb.service.eda.access.service.user.EndUserPatchService;
import org.veupathdb.service.eda.access.service.user.EndUserSearchService;

import java.util.List;

import static org.veupathdb.service.eda.access.service.provider.ProviderService.userIsManager;
import static org.veupathdb.service.eda.access.service.staff.StaffService.userIsOwner;

@Authenticated
public class EndUserController implements DatasetEndUsers
{
  @Context
  ContainerRequest _request;

  @Override
  public GetDatasetEndUsersResponse getDatasetEndUsers(
    final String datasetId,
    final Long limit,
    final Long offset,
    final ApprovalStatus approval
  ) {
    return GetDatasetEndUsersResponse.respond200WithApplicationJson(
      EndUserSearchService.getInstance().findEndUsers(datasetId, limit, offset, approval, _request));
  }

  @Override
  public PostDatasetEndUsersResponse postDatasetEndUsers(final EndUserCreateRequest entity) {
    return PostDatasetEndUsersResponse.respond200WithApplicationJson(
      EndUserCreationService.getInstance().handleUserCreation(entity, _request));
  }

  @Override
  public GetDatasetEndUsersByEndUserIdResponse getDatasetEndUsersByEndUserId(
    final String endUserId
  ) {
    final var curUser = Util.requireUser(_request);
    final var endUser = EndUserLookupService.getEndUser(endUserId);

    if (endUser.getUser().getUserId() == curUser.getUserID()
      || ProviderService.getInstance().isUserProvider(curUser.getUserID(), endUser.getDatasetId())
      || userIsOwner(curUser.getUserID())
    ) {
      return GetDatasetEndUsersByEndUserIdResponse.respond200WithApplicationJson(
        endUser);
    }

    throw new ForbiddenException();
  }

  @Override
  public PatchDatasetEndUsersByEndUserIdResponse patchDatasetEndUsersByEndUserId(
    final String endUserId,
    final List < EndUserPatch > entity
  ) {
    final var curUser = Util.requireUser(_request);
    final var endUser = EndUserLookupService.getRawEndUser(endUserId);

    if (endUser.getUserId() == curUser.getUserID()) { // Users can edit some of the fields of their request.
      EndUserPatchService.selfPatch(endUser, entity, curUser.getUserID());
    } else if (userIsManager(curUser.getUserID(), endUser.getDatasetId()) || userIsOwner(curUser.getUserID())) {
      EndUserPatchService.modPatch(endUser, entity, curUser.getUserID());
    } else {
      throw new ForbiddenException();
    }

    return PatchDatasetEndUsersByEndUserIdResponse.respond204();
  }

  @Override
  public DeleteDatasetEndUsersByEndUserIdResponse deleteDatasetEndUsersByEndUserId(String endUserId)
  {
    try {
      final var curUser = Util.requireUser(_request);
      final var endUser = EndUserLookupService.getRawEndUser(endUserId);

      if (userIsManager(curUser.getUserID(), endUser.getDatasetId()) || userIsOwner(curUser.getUserID())) {
        EndUserDeleteService.delete(endUser, curUser.getUserID());
      }

      return DeleteDatasetEndUsersByEndUserIdResponse.respond204();
    } catch (BadRequestException ex) {
      throw new NotFoundException();
    }
  }
}
