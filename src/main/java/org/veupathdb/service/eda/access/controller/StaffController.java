package org.veupathdb.service.eda.access.controller;

import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.core.Context;
import org.glassfish.jersey.server.ContainerRequest;
import org.veupathdb.lib.container.jaxrs.server.annotations.Authenticated;
import org.veupathdb.service.eda.generated.model.NewStaffRequest;
import org.veupathdb.service.eda.generated.model.NewStaffResponseImpl;
import org.veupathdb.service.eda.generated.model.StaffPatch;
import org.veupathdb.service.eda.generated.resources.Staff;
import org.veupathdb.service.eda.access.service.staff.StaffService;
import org.veupathdb.service.eda.access.util.Keys;

import java.util.List;
import java.util.Map;

@Authenticated
public class StaffController implements Staff
{
  @Context
  ContainerRequest _request;

  @Override
  public GetStaffResponse getStaff(final Long limit, final Long offset) {
    if (!StaffService.userIsStaff(_request))
      throw new ForbiddenException();

    return GetStaffResponse.respond200WithApplicationJson(
      StaffService.getStaff(limit, offset));
  }

  @Override
  public PostStaffResponse postStaff(final NewStaffRequest entity) {
    if (!StaffService.userIsOwner(_request))
      throw new ForbiddenException();

    final var out = new NewStaffResponseImpl();
    out.setStaffId(StaffService.createStaff(entity));

    return PostStaffResponse.respond200WithApplicationJson(out);
  }

  @SuppressWarnings("unchecked")
  @Override
  public PatchStaffByStaffIdResponse patchStaffByStaffId(
    final Long staffId,
    final List < StaffPatch > entity
  ) {
    if (!StaffService.userIsOwner(_request))
      throw new ForbiddenException();

    StaffService.validatePatch(entity);

    final var row = StaffService.requireStaffById(staffId);

    // WARNING: This cast mess is due to a bug in the JaxRS generator, the type
    // it actually passes up is not the declared type, but a list of linked hash
    // maps instead.
    final var item = ((List< Map <String, Object> >)((Object) entity)).get(0);


    row.setOwner((boolean) item.get(Keys.Json.KEY_VALUE));
    StaffService.updateStaffRow(row);

    return PatchStaffByStaffIdResponse.respond204();
  }

  @Override
  public DeleteStaffByStaffIdResponse deleteStaffByStaffId(final Long staffId) {
    if (!StaffService.userIsOwner(_request))
      throw new ForbiddenException();

    StaffService.deleteStaff(staffId);

    return DeleteStaffByStaffIdResponse.respond204();
  }
}
