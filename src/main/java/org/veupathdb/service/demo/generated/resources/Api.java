package org.veupathdb.service.demo.generated.resources;

import org.veupathdb.service.demo.generated.support.ResponseDelegate;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

@Path("/api")
public interface Api {
  @GET
  @Produces("text/html")
  GetApiResponse getApi();

  class GetApiResponse extends ResponseDelegate {
    private GetApiResponse(Response response, Object entity) {
      super(response, entity);
    }

    private GetApiResponse(Response response) {
      super(response);
    }

    public static GetApiResponse respond200WithTextHtml(Object entity) {
      Response.ResponseBuilder responseBuilder = Response.status(200).header("Content-Type", "text/html");
      responseBuilder.entity(entity);
      return new GetApiResponse(responseBuilder.build(), entity);
    }
  }
}
