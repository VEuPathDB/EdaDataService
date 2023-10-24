package org.veupathdb.service.eda.access.service.user;

import jakarta.ws.rs.InternalServerErrorException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.veupathdb.service.eda.access.model.EndUserRow;

public class EndUserDeleteService
{
  private static final Logger log = LogManager.getLogger(EndUserDeleteService.class);

  private static EndUserDeleteService instance;

  public static EndUserDeleteService getInstance() {
    log.trace("EndUserDeleteService#getInstance()");

    if (instance == null)
      return instance = new EndUserDeleteService();
    return instance;
  }

  public static void delete(EndUserRow user, long causerId) {
    getInstance().deleteGrant(user, causerId);
  }

  public void deleteGrant(EndUserRow user, long causerId) {
    log.trace("EndUserDeleteService#deleteGrant(EndUserRow)");

    try {
      EndUserRepo.Delete.endUser(user, causerId);
    } catch (Exception ex) {
      throw new InternalServerErrorException(ex);
    }
  }
}
