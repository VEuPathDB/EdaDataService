package org.veupathdb.service.eda.access.service.user;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import org.apache.logging.log4j.Logger;
import org.veupathdb.lib.container.jaxrs.providers.LogProvider;
import org.veupathdb.service.eda.generated.model.EndUser;
import org.veupathdb.service.eda.access.model.EndUserRow;

public class EndUserLookupService
{
  @SuppressWarnings("FieldMayBeFinal")
  private static EndUserLookupService instance = new EndUserLookupService();

  private final Logger log = LogProvider.logger(EndUserLookupService.class);

  EndUserLookupService() {
  }

  // ╔════════════════════════════════════════════════════════════════════╗ //
  // ║                                                                    ║ //
  // ║    End User Lookup Handling                                        ║ //
  // ║                                                                    ║ //
  // ╚════════════════════════════════════════════════════════════════════╝ //

  public EndUser lookupEndUser(final String rawId) {
    log.trace("EndUserService#getEndUser(String)");

    final var id = new EndUserId(rawId);

    try {
      return EndUserUtil.row2EndUser(EndUserRepo.Select.endUser(id.userId, id.datasetId)
        .orElseThrow(NotFoundException::new));
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      throw new InternalServerErrorException(e);
    }
  }

  public static EndUser getEndUser(final String rawId) {
    return getInstance().lookupEndUser(rawId);
  }

  public EndUserRow lookupRawEndUser(final String rawId) {
    log.trace("EndUserService#getRawEndUser(String)");

    final var id = new EndUserId(rawId);

    try {
      return EndUserRepo.Select.endUser(id.userId, id.datasetId)
        .orElseThrow(BadRequestException::new);
    } catch (Exception e) {
      throw new InternalServerErrorException(e);
    }
  }

  public static EndUserRow getRawEndUser(final String rawId) {
    return getInstance().lookupRawEndUser(rawId);
  }

////////////////////////////////////////////////////////////////////////////////

  public boolean checkEndUserExists(final long userId, final String datasetId) {
    log.trace("EndUserService#checkEndUserExists(long, String)");

    try {
      return EndUserRepo.Select.endUser(userId, datasetId).isPresent();
    } catch (Exception e) {
      throw new InternalServerErrorException(e);
    }
  }

  public static EndUserLookupService getInstance() {
    return instance;
  }

  private static class EndUserId
  {
    private final long userId;
    private final String datasetId;

    private EndUserId(final String rawId) {
      final var ind = rawId.indexOf('-');

      if (ind == -1)
        throw new BadRequestException();

      try {
        userId = Long.parseLong(rawId.substring(0, ind));
      } catch (Exception e) {
        throw new BadRequestException(e);
      }

      datasetId = rawId.substring(ind + 1);
    }
  }
}
