package org.veupathdb.service.eda.access.service.staff;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.server.ContainerRequest;
import org.veupathdb.lib.container.jaxrs.providers.LogProvider;
import org.veupathdb.lib.container.jaxrs.providers.UserProvider;
import org.veupathdb.service.eda.generated.model.NewStaffRequest;
import org.veupathdb.service.eda.generated.model.Staff;
import org.veupathdb.service.eda.generated.model.StaffImpl;
import org.veupathdb.service.eda.generated.model.StaffList;
import org.veupathdb.service.eda.generated.model.StaffListImpl;
import org.veupathdb.service.eda.generated.model.StaffPatch;
import org.veupathdb.service.eda.generated.model.UserDetailsImpl;
import org.veupathdb.service.eda.access.model.PartialStaffRow;
import org.veupathdb.service.eda.access.model.StaffRow;
import org.veupathdb.service.eda.access.util.Keys;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class StaffService {

  private static final StaffService instance = new StaffService();

  private final Logger log = LogProvider.logger(StaffService.class);

  StaffService() {}

  public static StaffService getInstance() {
    return instance;
  }

  /**
   * Looks up the current user and checks if they are a site owner.
   *
   * @return whether or not the current user is a site owner.
   */
  public boolean isUserOwner(final ContainerRequest req) {
    log.trace("StaffService#isUserOwner(Request)");

    return isUserOwner(UserProvider.lookupUser(req)
      .orElseThrow(InternalServerErrorException::new)
      .getUserID());
  }

  public static boolean userIsOwner(final ContainerRequest req) {
    return getInstance().isUserOwner(req);
  }

  public static boolean userIsStaff(final ContainerRequest req) {
    return getInstance().isUserStaff(req);
  }

  public boolean isUserStaff(final ContainerRequest req) {
    log.trace("StaffService#isUserStaff(Request)");

    return isUserStaff(UserProvider.lookupUser(req)
      .orElseThrow(InternalServerErrorException::new)
      .getUserID());
  }

  public static boolean userIsStaff(final long id) {
    return getInstance().isUserStaff(id);
  }

  public boolean isUserStaff(final long userId) {
    log.trace("StaffService#isUserStaff(long)");

    try {
      return StaffRepo.Select.byUserId(userId).isPresent();
    } catch (WebApplicationException e) {
      throw e;
    } catch (Throwable e) {
      throw new InternalServerErrorException(e);
    }
  }

  /**
   * Looks up whether the given userId belongs to a site owner.
   *
   * @return whether or not the given userId belongs to a site owner.
   */
  public boolean isUserOwner(final long userId) {
    log.trace("StaffService#isUserOwner(long)");

    try {
      return StaffRepo.Select.byUserId(userId).filter(StaffRow::isOwner).isPresent();
    } catch (WebApplicationException e) {
      throw e;
    } catch (Throwable e) {
      throw new InternalServerErrorException(e);
    }
  }

  /**
   * Checks whether staff user is an "owner", meaning they are allowed to manage access requests on behalf of providers.
   */
  public static boolean userIsOwner(final long userId) {
    return getInstance().isUserOwner(userId);
  }

  public void updateStaffRecord(final StaffRow row) {
    log.trace("StaffService#updateStaffRecord(StaffRow)");
    try {
      StaffRepo.Update.ownerFlagById(row);
    } catch (Throwable e) {
      throw new InternalServerErrorException(e);
    }
  }

  public static void updateStaffRow(final StaffRow row) {
    getInstance().updateStaffRecord(row);
  }

  public StaffList getStaffList(final long limit, final long offset) {
    log.trace("StaffService#getStaffList(long, long)");

    try {
      return rows2StaffList(StaffRepo.Select.list(limit, offset), offset, StaffRepo.Select.count());
    } catch (Throwable e) {
      throw new InternalServerErrorException(e);
    }
  }

  public static StaffList getStaff(final long limit, final long offset) {
    return getInstance().getStaffList(limit, offset);
  }

  public StaffRow mustGetStaffById(final Long staffId) {
    log.trace("StaffService#mustGetStaffById(int)");

    try {
      return StaffRepo.Select.byId(staffId).orElseThrow(NotFoundException::new);
    } catch (Throwable e) {
      throw new InternalServerErrorException(e);
    }
  }

  public static StaffRow requireStaffById(final Long staffId) {
    return getInstance().mustGetStaffById(staffId);
  }

  public long createNewStaff(final NewStaffRequest req) {
    log.trace("StaffService#createStaff(NewStaffRequest)");

    final var row = new PartialStaffRow();

    row.setUserId(req.getUserId());
    row.setOwner(req.getIsOwner());

    try {
      return StaffRepo.Insert.newStaff(row);
    } catch (Throwable e) {
      throw new InternalServerErrorException(e);
    }
  }

  public static long createStaff(final NewStaffRequest req) {
    return getInstance().createNewStaff(req);
  }


  public void deleteStaffRecord(final Long staffId) {
    log.trace("StaffService#deleteStaff(int)");

    try {
      StaffRepo.Delete.byId(staffId);
    } catch (Exception e) {
      throw new InternalServerErrorException(e);
    }
  }

  public static void deleteStaff(final Long staffId) {
    getInstance().deleteStaffRecord(staffId);
  }

  @SuppressWarnings("unchecked")
  public void validatePatchRequest(final List < StaffPatch > entity) {
    log.trace("StaffService#validatePatchRequest(List)");

    if (entity == null || entity.isEmpty())
      throw new BadRequestException();

    if (entity.size() > 1)
      throw new ForbiddenException();

    // WARNING: This cast mess is due to a bug in the JaxRS generator, the type
    // it actually passes up is not the declared type, but a list of linked hash
    // maps instead.
    final var mod = ((List< Map <String, Object> >)((Object) entity)).get(0);

    if (!"replace".equals(mod.get(Keys.Json.KEY_OP)))
      throw new ForbiddenException();

    if (!("/" + Keys.Json.KEY_IS_OWNER).equals(mod.get(Keys.Json.KEY_PATH)))
      throw new ForbiddenException();
  }

  public static void validatePatch(final List < StaffPatch > entity) {
    getInstance().validatePatchRequest(entity);
  }

  private StaffList rows2StaffList(
    final List < StaffRow > rows,
    final long offset,
    final long total
  ) {
    log.trace("StaffService#rows2StaffList(rows, offset, total)");

    final var out = new StaffListImpl();

    out.setOffset(offset);
    out.setTotal(total);
    out.setRows((long)rows.size());
    out.setData(rows.stream()
      .map(this::row2Staff)
      .collect(Collectors.toList()));

    return out;
  }

  private Staff row2Staff(final StaffRow row) {
    log.trace("StaffService#row2Staff(row)");

    final var user = new UserDetailsImpl();
    user.setOrganization(row.getOrganization());
    user.setFirstName(row.getFirstName());
    user.setLastName(row.getLastName());
    user.setUserId(row.getUserId());
    user.setEmail(row.getEmail());

    final var out = new StaffImpl();
    out.setIsOwner(row.isOwner());
    out.setStaffId(row.getStaffId());
    out.setUser(user);

    return out;
  }
}
