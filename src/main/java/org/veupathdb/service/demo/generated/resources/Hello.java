package org.veupathdb.service.demo.generated.resources;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import org.veupathdb.service.demo.generated.model.HelloPostRequest;
import org.veupathdb.service.demo.generated.model.HelloPostResponse;
import org.veupathdb.service.demo.generated.model.HelloResponse;
import org.veupathdb.service.demo.generated.model.ServerErrorResponse;
import org.veupathdb.service.demo.generated.model.UnauthorizedResponse;
import org.veupathdb.service.demo.generated.support.ResponseDelegate;

@Path("/hello")
public interface Hello {
  @GET
  @Produces("application/json")
  GetHelloResponse getHello();

  @POST
  @Produces("application/json")
  @Consumes("application/json")
  PostHelloResponse postHello(HelloPostRequest entity);

  class PostHelloResponse extends ResponseDelegate {
    private PostHelloResponse(Response response, Object entity) {
      super(response, entity);
    }

    private PostHelloResponse(Response response) {
      super(response);
    }

    public static PostHelloResponse respond200WithApplicationJson(HelloPostResponse entity) {
      Response.ResponseBuilder responseBuilder = Response.status(200).header("Content-Type", "application/json");
      responseBuilder.entity(entity);
      return new PostHelloResponse(responseBuilder.build(), entity);
    }

    public static PostHelloResponse respond401WithApplicationJson(UnauthorizedResponse entity) {
      Response.ResponseBuilder responseBuilder = Response.status(401).header("Content-Type", "application/json");
      responseBuilder.entity(entity);
      return new PostHelloResponse(responseBuilder.build(), entity);
    }

    public static PostHelloResponse respond500WithApplicationJson(ServerErrorResponse entity) {
      Response.ResponseBuilder responseBuilder = Response.status(500).header("Content-Type", "application/json");
      responseBuilder.entity(entity);
      return new PostHelloResponse(responseBuilder.build(), entity);
    }
  }

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
