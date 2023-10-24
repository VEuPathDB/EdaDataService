package org.veupathdb.service.eda.access.controller;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.core.Context;
import org.glassfish.jersey.server.ContainerRequest;
import org.veupathdb.lib.container.jaxrs.server.annotations.Authenticated;
import org.veupathdb.service.eda.generated.model.DatasetProviderCreateRequest;
import org.veupathdb.service.eda.generated.model.DatasetProviderPatch;
import org.veupathdb.service.eda.generated.resources.DatasetProviders;
import org.veupathdb.service.eda.access.service.provider.ProviderService;

import java.util.List;

import static org.veupathdb.service.eda.access.service.provider.ProviderService.*;
import static org.veupathdb.service.eda.access.service.staff.StaffService.userIsOwner;

@Authenticated
public class ProviderController implements DatasetProviders
{
  @Context
  ContainerRequest _request;

  @Override
  public GetDatasetProvidersResponse getDatasetProviders(
    final String datasetId,
    final Long limit,
    final Long offset
  ) {
    final var currentUser = Util.requireUser(_request);

    if (datasetId == null || datasetId.isBlank())
      throw new BadRequestException("datasetId query param is required");

    return GetDatasetProvidersResponse.respond200WithApplicationJson(
      getProviderList(datasetId, limit, offset, currentUser));
  }

  @Override
  public PostDatasetProvidersResponse postDatasetProviders(
    final DatasetProviderCreateRequest entity
  ) {
    return PostDatasetProvidersResponse.respond200WithApplicationJson(
      ProviderService.getInstance().createNewProvider(entity, Util.requireUser(_request)));
  }

  @Override
  public PatchDatasetProvidersByProviderIdResponse patchDatasetProvidersByProviderId(
    final Long providerId,
    final List <DatasetProviderPatch> entity
  ) {
    final var currentUser = Util.requireUser(_request);

    ProviderService.getInstance().validatePatchRequest(entity);

    final var provider = requireProviderById(providerId);

    // To add a new provider, a user must be a site owner or a manager for the
    // dataset.
    if (!userIsOwner(currentUser.getUserID()) && !userIsManager(currentUser.getUserID(), provider.getDatasetId()))
      throw new ForbiddenException();

    provider.setManager(entity.get(0).getValue());
    updateProvider(provider);

    return PatchDatasetProvidersByProviderIdResponse.respond204();
  }

  @Override
  public DeleteDatasetProvidersByProviderIdResponse deleteDatasetProvidersByProviderId(
    final Long providerId
  ) {
    if (!userIsOwner(_request))
      throw new ForbiddenException();

    // Lookup will 404 if the provider id is invalid.
    requireProviderById(providerId);
    deleteProvider(providerId);

    return DeleteDatasetProvidersByProviderIdResponse.respond204();
  }
}
