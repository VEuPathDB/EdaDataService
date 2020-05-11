package org.veupathdb.service.demo.container.service;

import org.veupathdb.service.demo.generated.resources.Api;

import javax.ws.rs.core.StreamingOutput;

public class ApiDocService implements Api {
  @Override
  public GetApiResponse getApi() {
    return GetApiResponse.respond200WithTextHtml(streamApiDoc());
  }

  static StreamingOutput streamApiDoc() {
    return Api.class.getResourceAsStream("/api.html")::transferTo;
  }
}
