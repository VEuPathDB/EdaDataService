package org.veupathdb.service.eda.access.service.user;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.WebApplicationException;
import org.apache.logging.log4j.Logger;
import org.veupathdb.lib.container.jaxrs.providers.LogProvider;
import org.veupathdb.service.eda.Main;
import org.veupathdb.service.eda.generated.model.EndUserPatch;
import org.veupathdb.service.eda.generated.model.EndUserPatch.OpType;
import org.veupathdb.service.eda.access.model.ApprovalStatus;
import org.veupathdb.service.eda.access.model.EndUserRow;
import org.veupathdb.service.eda.access.model.ProviderRow;
import org.veupathdb.service.eda.access.model.RestrictionLevel;
import org.veupathdb.service.eda.access.model.UserRow;
import org.veupathdb.service.eda.access.service.account.AccountRepo;
import org.veupathdb.service.eda.access.service.dataset.DatasetRepo;
import org.veupathdb.service.eda.access.service.email.EmailService;
import org.veupathdb.service.eda.access.service.provider.ProviderRepo;
import org.veupathdb.service.eda.access.util.Keys;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class EndUserPatchService
{
  private static final String
    ERR_NOT_EDITABLE = "This record is not marked as editable.";

  @SuppressWarnings("FieldMayBeFinal")
  private static EndUserPatchService instance;

  private final Logger log = LogProvider.logger(EndUserPatchService.class);

  EndUserPatchService() {
  }

  // ╔════════════════════════════════════════════════════════════════════╗ //
  // ║                                                                    ║ //
  // ║    End User Lookup Handling                                        ║ //
  // ║                                                                    ║ //
  // ╚════════════════════════════════════════════════════════════════════╝ //

  public void applySelfPatch(
    final EndUserRow row,
    final List<EndUserPatch> patches,
    final long userID
  ) {
    log.trace("EndUserService#selfPatch(EndUserRow, List)");
    var pVal = new PatchUtil();

    if (patches == null || patches.isEmpty())
      throw new BadRequestException();

    if (!row.isAllowSelfEdits())
      throw new ForbiddenException(ERR_NOT_EDITABLE);

    for (var patch : patches) {
      // End users are only permitted to perform "replace" patch operations.
      pVal.enforceOpIn(patch, OpType.REPLACE);

      switch (patch.getPath().substring(1)) {
        case Keys.Json.KEY_PURPOSE
          -> pVal.strVal(patch, row::setPurpose);

        case Keys.Json.KEY_RESEARCH_QUESTION
          -> pVal.strVal(patch, row::setResearchQuestion);

        case Keys.Json.KEY_ANALYSIS_PLAN
          -> pVal.strVal(patch, row::setAnalysisPlan);

        case Keys.Json.KEY_DISSEMINATION_PLAN
          -> pVal.strVal(patch, row::setDisseminationPlan);

        case Keys.Json.KEY_PRIOR_AUTH
          -> pVal.strVal(patch, row::setPriorAuth);

        // do nothing
        default -> throw pVal.forbiddenOp(patch);
      }
    }

    // End users are only allowed to edit their access request once without a
    // manager or provider stepping in to re-enable self edits.
    row.setAllowSelfEdits(false);

    // Set approval status to requested when a self-patch is made.
    row.setApprovalStatus(ApprovalStatus.REQUESTED);

    try {
      final var ds = DatasetRepo.Select.getInstance()
        .selectDataset(row.getDatasetId())
        .orElseThrow();
      Optional<String> userEmail = AccountRepo.Select.getInstance().selectEmailByUserId(row.getUserId());
      final var ccs = ProviderRepo.Select.byDataset(row.getDatasetId(), 100L, 0L).stream()
        .map(UserRow::getEmail)
        .toArray(String[]::new);
      EndUserRepo.Update.self(row, userID);
      EmailService.getInstance()
        .sendEndUserUpdateNotificationEmail(ccs, ds, row, ds.getRequestEmailBodyManager());

      if (userEmail.isPresent()) {
        EmailService.getInstance()
            .sendEndUserUpdateNotificationEmail(new String[]{ userEmail.get() }, ds, row, ds.getRequestEmailBodyRequester());
      }

    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      throw new InternalServerErrorException(e);
    }
  }

  public static void selfPatch(
    final EndUserRow row,
    final List<EndUserPatch> patches,
    final long userID
  ) {
    getInstance().applySelfPatch(row, patches, userID);
  }

  public void applyModPatch(
    final EndUserRow row,
    final List<EndUserPatch> patches,
    final long userID
  ) {
    log.trace("EndUserService#modPatch(row, patches)");
    var pVal = new PatchUtil();
    boolean approved = false;
    boolean denied = false;

    if (patches.isEmpty())
      throw new BadRequestException();

    for (var patch : patches) {
      pVal.enforceOpIn(patch, OpType.ADD, OpType.REMOVE, OpType.REPLACE);

      // Remove the leading '/' character from the path.
      switch (patch.getPath().substring(1)) {
        case Keys.Json.KEY_START_DATE -> {
          if (patch.getValue() == null)
            row.setStartDate(null);
          else
            row.setStartDate(OffsetDateTime.parse(
              pVal.enforceType(patch.getValue(), String.class)));
        }
        case Keys.Json.KEY_DURATION -> {
          if (patch.getValue() == null)
            row.setDuration(-1);
          else
            row.setDuration(pVal.enforceType(patch.getValue(), Number.class).intValue());
        }
        case Keys.Json.KEY_PURPOSE -> pVal.strVal(patch, row::setPurpose);
        case Keys.Json.KEY_RESEARCH_QUESTION -> pVal.strVal(patch, row::setResearchQuestion);
        case Keys.Json.KEY_ANALYSIS_PLAN -> pVal.strVal(patch, row::setAnalysisPlan);
        case Keys.Json.KEY_DISSEMINATION_PLAN -> pVal.strVal(patch, row::setDisseminationPlan);
        case Keys.Json.KEY_PRIOR_AUTH -> pVal.strVal(patch, row::setPriorAuth);
        case Keys.Json.KEY_RESTRICTION_LEVEL -> pVal.enumVal(
              patch,
              RestrictionLevel::valueOf,
              row::setRestrictionLevel
          );
        case Keys.Json.KEY_APPROVAL_STATUS -> {
          approved = ApprovalStatus.valueOf(patch.getValue().toString().toUpperCase()) == ApprovalStatus.APPROVED;
          denied = ApprovalStatus.valueOf(patch.getValue().toString().toUpperCase()) == ApprovalStatus.DENIED;
          // Allow a single self-edit after a request is rejected.
          if (denied) {
            log.info("Allowing self-edits because request was {}.", patch.getValue().toString());
            row.setAllowSelfEdits(true);
          }
          pVal.enumVal(
              patch,
              ApprovalStatus::valueOf,
              row::setApprovalStatus
          );
        }
        case Keys.Json.KEY_DENIAL_REASON -> pVal.strVal(patch, row::setDenialReason);
        default -> throw pVal.forbiddenOp(patch);
      }
    }

    try {
      EndUserRepo.Update.mod(row, userID);
    } catch (Exception e) {
      throw new InternalServerErrorException(e);
    }

    // Send an e-mail to the end-user whose request was approved or denied.
    try {
      final var ds = DatasetRepo.Select.getInstance()
          .selectDataset(row.getDatasetId())
          .orElseThrow();
      Optional<String> userEmail = AccountRepo.Select.getInstance().selectEmailByUserId(row.getUserId());

      final var managerEmails = ProviderRepo.Select.byDataset(row.getDatasetId(), 100L, 0L).stream()
          .filter(ProviderRow::isManager)
          .map(UserRow::getEmail)
          .toArray(String[]::new);

      // Send to the approved/denied user in the e-mail notification as well as managers and support.
      String[] ccs = Stream.concat(
          Stream.concat(userEmail.stream(), Arrays.stream(managerEmails)),
              Stream.of(Main.config.getSupportEmail()))
          .toArray(String[]::new);
      if (approved) {
        EmailService.getInstance().sendDatasetApprovedNotificationEmail(ccs, ds, row, managerEmails);
      } else if (denied) {
        EmailService.getInstance().sendDatasetDeniedNotificationEmail(ccs, ds, row, managerEmails);
      } else {
        log.debug("No need to send an e-mail notification, as patch request did not update approval status.");
      }
    } catch (Exception e) {
      log.error("Failed to send e-mail to user {}", userID, e);
      throw new RuntimeException(e);
    }
  }

  public static void modPatch(
    final EndUserRow row,
    final List<EndUserPatch> patches,
    final long userID
  ) {
    getInstance().applyModPatch(row, patches, userID);
  }

  public static EndUserPatchService getInstance() {
    if (instance == null)
      instance = new EndUserPatchService();

    return instance;
  }
}
