package org.veupathdb.service.eda.access.controller;

import jakarta.ws.rs.core.Context;
import org.glassfish.jersey.server.ContainerRequest;
import org.veupathdb.lib.container.jaxrs.server.annotations.Authenticated;
import org.veupathdb.service.eda.generated.resources.History;
import org.veupathdb.service.eda.access.service.history.HistoryService;

@Authenticated
public class HistoryController implements History
{
  @Context
  ContainerRequest _request;

  @Override
  public GetHistoryResponse getHistory(Long limit, Long offset) {
    var user = Util.requireUser(_request);

    return GetHistoryResponse.respond200WithApplicationJson(
      HistoryService.getHistory(user.getUserID(), limit, offset)
    );
  }
}
