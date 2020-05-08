package org.veupathdb.service.demo.resources;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import org.veupathdb.service.demo.model.HelloResponse;
import org.veupathdb.service.demo.support.ResponseDelegate;

@Path("/hello")
public interface Hello {
  @GET
  @Produces("application/json")
  GetHelloResponse getHello();

  class GetHelloResponse extends ResponseDelegate {
    private GetHelloResponse(Response response, Object entity) {
      super(response, entity);
    }

    private GetHelloResponse(Response response) {
      super(response);
    }

    public static GetHelloResponse respond200WithApplicationJson(HelloResponse entity) {
      Response.ResponseBuilder responseBuilder = Response.status(200).header("Content-Type", "application/json");
      responseBuilder.entity(entity);
      return new GetHelloResponse(responseBuilder.build(), entity);
    }
  }
}
