package org.veupathdb.service.eda.access.service.user;

import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.WebApplicationException;
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.server.ContainerRequest;
import org.veupathdb.lib.container.jaxrs.providers.LogProvider;
import org.veupathdb.lib.container.jaxrs.providers.UserProvider;
import org.veupathdb.service.eda.access.model.SearchQuery;
import org.veupathdb.service.eda.access.service.provider.ProviderRepo;
import org.veupathdb.service.eda.access.service.staff.StaffRepo;
import org.veupathdb.service.eda.access.service.staff.StaffService;
import org.veupathdb.service.eda.generated.model.ApprovalStatus;
import org.veupathdb.service.eda.generated.model.EndUserList;

public class EndUserSearchService
{
  private static EndUserSearchService instance;

  private final Logger log = LogProvider.logger(EndUserSearchService.class);

  EndUserSearchService() {
  }

  public static EndUserSearchService getInstance() {
    if (instance == null)
      instance = new EndUserSearchService();

    return instance;
  }

  // ╔════════════════════════════════════════════════════════════════════╗ //
  // ║                                                                    ║ //
  // ║    End User Search Handling                                        ║ //
  // ║                                                                    ║ //
  // ╚════════════════════════════════════════════════════════════════════╝ //

  /**
   * Returns an {@link EndUserList} result containing at most <code>limit</code>
   * end users that have access to the given <code>datasetId</code> starting at
   * result <code>offset + 1</code>, optionally filtered by an approval status.
   *
   * @param datasetId ID of the dataset for which to find end users.
   * @param limit     Max number of results to return
   * @param offset    Record offset.
   * @param approval  Optional (nullable) approval status filter.
   *
   * @return limited result set to return to the client.
   */
  public EndUserList findEndUsers(
    final String datasetId,
    final Long limit,
    final Long offset,
    final ApprovalStatus approval,
    final ContainerRequest request
    ) {
    log.trace("EndUserSearchService#findEndUsers(String, int, int, ApprovalStatus)");

    final var user = UserProvider.lookupUser(request)
      .orElseThrow(() -> new NotAuthorizedException("Users must be logged in"));

    final var isOwner = StaffService.userIsOwner(user.getUserID());

    try {
      // Only owners may request a full listing of users
      if (datasetId == null) {
        if (!isOwner)
          throw new ForbiddenException();
      } else {
        if (!isOwner
          && ProviderRepo.Select.byUserAndDataset(user.getUserID(), datasetId).isEmpty()
          && StaffRepo.Select.byUserId(user.getUserID()).isEmpty()
        )
          throw new ForbiddenException();
      }

      final var query = new SearchQuery()
        .setDatasetId(datasetId)
        .setLimit(limit)
        .setOffset(offset)
        .setApprovalStatus(EndUserUtil.convertApproval(approval));

      return EndUserUtil.rows2EndUserList(
        EndUserRepo.Select.find(query),
        offset,
        EndUserRepo.Select.count(query)
      );
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      throw new InternalServerErrorException(e);
    }
  }
}
