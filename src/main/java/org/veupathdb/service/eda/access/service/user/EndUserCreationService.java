package org.veupathdb.service.eda.access.service.user;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.WebApplicationException;
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.server.ContainerRequest;
import org.veupathdb.lib.container.jaxrs.errors.UnprocessableEntityException;
import org.veupathdb.lib.container.jaxrs.providers.LogProvider;
import org.veupathdb.lib.container.jaxrs.providers.UserProvider;
import org.veupathdb.service.eda.generated.model.EndUserCreateRequest;
import org.veupathdb.service.eda.generated.model.EndUserCreateResponse;
import org.veupathdb.service.eda.generated.model.EndUserCreateResponseImpl;
import org.veupathdb.service.eda.access.model.ApprovalStatus;
import org.veupathdb.service.eda.access.model.EndUserRow;
import org.veupathdb.service.eda.access.model.RestrictionLevel;
import org.veupathdb.service.eda.access.service.account.AccountRepo;
import org.veupathdb.service.eda.access.service.dataset.DatasetRepo;
import org.veupathdb.service.eda.access.service.email.EmailService;
import org.veupathdb.service.eda.access.service.provider.ProviderRepo;
import org.veupathdb.service.eda.access.service.staff.StaffService;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static org.veupathdb.service.eda.access.util.Keys.Json.*;

public class EndUserCreationService
{
  private static final String
    ERR_DUPLICATE_USER = "The given end user already has an access request for the given dataset.";

  private static EndUserCreationService instance;

  private static final Logger log = LogProvider.logger(EndUserCreationService.class);

  EndUserCreationService() {
  }

  public static EndUserCreationService getInstance() {
    if (instance == null)
      instance = new EndUserCreationService();

    return instance;
  }

  // ╔════════════════════════════════════════════════════════════════════╗ //
  // ║                                                                    ║ //
  // ║    End User Creation Start                                         ║ //
  // ║                                                                    ║ //
  // ╚════════════════════════════════════════════════════════════════════╝ //

  public EndUserCreateResponse handleUserCreation(
    final EndUserCreateRequest body,
    final ContainerRequest request
  ) {
    final var requester = UserProvider.lookupUser(request)
      .orElseThrow(() -> new NotAuthorizedException("User must be logged in."));

    lightCreationCheck(body);

    try {
      final var optDs = DatasetRepo.Select.getInstance().selectDataset(body.getDatasetId());

      // Verify the dataset exists.
      if (optDs.isEmpty())
        throw new UnprocessableEntityException(Collections.singletonMap(
          KEY_DATASET_ID,
          Collections.singletonList("Invalid dataset id.")
        ));

      // If the request was sent using an email instead of a user id, lookup
      // the user's id.  If they don't have an account, send an email and alert
      // the client that the record was not created.
      if (cReqHasEmail(body)) {
        var optId = AccountRepo.Select.getInstance().selectUserIdByEmail(body.getEmail());

        // User email was unrecognized.  Send an email and end here.
        if (optId.isEmpty()) {
          log.info("Will send email to " + body.getEmail() + " telling user to register");
          EmailService.getInstance().sendEndUserRegistrationEmail(body.getEmail(), optDs.get());

          final var out = new EndUserCreateResponseImpl();
          out.setCreated(false);
          return out;
        }

        body.setUserId(optId.get());
      }

      // Verify no such end user already exists.
      if (EndUserRepo.Select.endUser(body.getUserId(), body.getDatasetId()).isPresent())
        throw new BadRequestException(ERR_DUPLICATE_USER);

      // If the user sent this request in themself then pass to the self-create
      // specific logic.
      if (body.getUserId().equals(requester.getUserID()))
        return endUserSelfCreate(body, requester.getUserID());

      final var rUserId = requester.getUserID();
      final var optProv = ProviderRepo.Select.byUserAndDataset(rUserId, body.getDatasetId());

      // If the requesting user sent this in on behalf of someone else and the
      // requesting user is a provider or an owner then pass to the manager
      // create specific logic.
      if (optProv.isPresent() || StaffService.userIsOwner(rUserId))
        return endUserManagerCreate(body, rUserId);

      // If the request was sent on behalf of a user that is not the requesting
      // user, but the requesting user is not a provider or owner, throw a
      // 401.
      throw new ForbiddenException();
    } catch (WebApplicationException e) {
      // Pass jaxrs exceptions through.
      throw e;
    } catch (Exception e) {
      // Wrap non-jaxrs exceptions in a 500 error
      throw new InternalServerErrorException(e);
    }
  }

  // ╔════════════════════════════════════════════════════════════════════╗ //
  // ║    Validation                                                      ║ //
  // ╚════════════════════════════════════════════════════════════════════╝ //

  private static final String
    ERR_EMAIL_AND_USER_ID = "Only one of the fields [\"" + KEY_USER_ID + "\", \"" + KEY_EMAIL
    + "\"] may be provided when creating a new access request.",
    ERR_EMAIL_NOR_USER_ID = "Exactly one of the fields [\"" + KEY_USER_ID + "\", \"" + KEY_EMAIL
      + "\"] must be provided when creating a new access request.";

  /**
   * Performs lightweight sanity checks on the user creation request before
   * moving on to heavier operations involving the datastore.
   *
   * @param body request body to validate.
   *
   * @throws UnprocessableEntityException if the input fails one or more of the
   *                                      validation checks.
   */
  void lightCreationCheck(final EndUserCreateRequest body) {
    final var errs = new HashMap<String, List<String>>();

    // Dataset id must not be null, empty, or blank
    if (body.getDatasetId() == null || body.getDatasetId().isBlank())
      errs.put(KEY_DATASET_ID, Collections.singletonList("This field cannot be empty."));

    // Only one of the userId or email field may be used.
    final var userIdEmpty = cReqHasUserId(body);
    final var emailEmpty  = cReqHasEmail(body);

    if (userIdEmpty == emailEmpty) {
      var err = userIdEmpty ? ERR_EMAIL_AND_USER_ID : ERR_EMAIL_NOR_USER_ID;

      errs.put(KEY_USER_ID, Collections.singletonList(err));
      errs.put(KEY_EMAIL, Collections.singletonList(err));
    }

    if (!errs.isEmpty())
      throw new UnprocessableEntityException(errs);
  }

  // ╔════════════════════════════════════════════════════════════════════╗ //
  // ║    Utilities                                                       ║ //
  // ╚════════════════════════════════════════════════════════════════════╝ //

  boolean cReqHasUserId(final EndUserCreateRequest req) {
    return req.getUserId() != null && req.getUserId() > 0L;
  }

  boolean cReqHasEmail(final EndUserCreateRequest req) {
    return req.getEmail() != null && !req.getEmail().isBlank();
  }

  // ╔════════════════════════════════════════════════════════════════════╗ //
  // ║                                                                    ║ //
  // ║    End User Self Creation Start                                    ║ //
  // ║                                                                    ║ //
  // ╚════════════════════════════════════════════════════════════════════╝ //

  private static final String
    ERR_NOT_ALLOWED = "You do not have permission to use the field \"%s\"";

  /**
   * Inserts a new end user record with defaulted values in place of data that
   * cannot be self-set by end users.
   */
  public EndUserCreateResponse endUserSelfCreate(EndUserCreateRequest req, long userID) {
    log.trace("EndUserService#endUserSelfCreate(EndUserCreateRequest)");

    validateSelfCreate(req);

    try {
      final var row = EndUserUtil.createRequest2EndUserRow(req)
        .setApprovalStatus(ApprovalStatus.REQUESTED)
        .setRestrictionLevel(RestrictionLevel.PUBLIC)
        .setStartDate(OffsetDateTime.now())
        .setDuration(-1);

      EndUserRepo.Insert.newEndUser(row, userID);

      final var out = new EndUserCreateResponseImpl();
      out.setCreated(true);
      out.setEndUserId(EndUserUtil.formatEndUserId(row.getUserId(), row.getDatasetId()));
      return out;
    } catch (final WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      throw new InternalServerErrorException(e);
    }
  }

  void validateSelfCreate(final EndUserCreateRequest req) {
    if (req.getStartDate() != null)
      throw new ForbiddenException(String.format(ERR_NOT_ALLOWED, KEY_START_DATE));
    if (req.getDuration() != 0)
      throw new ForbiddenException(String.format(ERR_NOT_ALLOWED, KEY_DURATION));
    if (req.getRestrictionLevel() != null)
      throw new ForbiddenException(String.format(ERR_NOT_ALLOWED, KEY_RESTRICTION_LEVEL));
    if (req.getApprovalStatus() != null)
      throw new ForbiddenException(String.format(ERR_NOT_ALLOWED, KEY_APPROVAL_STATUS));
    if (req.getDenialReason() != null)
      throw new ForbiddenException(String.format(ERR_NOT_ALLOWED, KEY_DENIAL_REASON));
  }

  // ╔════════════════════════════════════════════════════════════════════╗ //
  // ║                                                                    ║ //
  // ║    End User Provider Creation Start                                ║ //
  // ║                                                                    ║ //
  // ╚════════════════════════════════════════════════════════════════════╝ //

  /**
   * Inserts a new end user record.
   * <p>
   * Provides defaults for the following fields if no value was set in the
   * request:
   * <ul>
   *   <li>startDate</li>
   *   <li>approvalStatus</li>
   *   <li>restrictionLevel</li>
   *   <li>duration</li>
   * </ul>
   */
  public EndUserCreateResponse endUserManagerCreate(EndUserCreateRequest req, long userID)
  throws Exception {
    log.trace("EndUserService#endUserManagerCreate(EndUserCreateRequest)");

    var row = endUserToInsertable(req);

    EndUserRepo.Insert.newEndUser(row, userID);

    var out = new EndUserCreateResponseImpl();
    out.setCreated(true);
    out.setEndUserId(EndUserUtil.formatEndUserId(row.getUserId(), row.getDatasetId()));

    return out;
  }

  EndUserRow endUserToInsertable(final EndUserCreateRequest req) {
    final var row = EndUserUtil.createRequest2EndUserRow(req);

    if (req.getStartDate() == null) {
      log.debug("defaulting start date");
      row.setStartDate(OffsetDateTime.now());
    }

    if (req.getApprovalStatus() == null) {
      log.debug("defaulting approval status");
      row.setApprovalStatus(ApprovalStatus.REQUESTED);
    }

    if (req.getRestrictionLevel() == null) {
      log.debug("defaulting restriction level");
      row.setRestrictionLevel(RestrictionLevel.PUBLIC);
    }

    if (req.getDuration() == 0)
      row.setDuration(-1);

    row.setAllowSelfEdits(true);

    return row;
  }
}
