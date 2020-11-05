package org.veupathdb.service.edads.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.Response.StatusType;

import org.gusdb.fgputil.IoUtil;
import org.gusdb.fgputil.Tuples.TwoTuple;
import org.gusdb.fgputil.functional.Either;
import org.veupathdb.service.edads.Resources;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class NetworkUtils {

  public static class RequestFailure extends TwoTuple<StatusType, String> {

    public RequestFailure(Response failureResponse) {
      super(failureResponse.getStatusInfo(), readResponseEntity(failureResponse));
    }

    private static String readResponseEntity(Response response) {
      try (Reader in = new InputStreamReader((InputStream)response.getEntity())) {
        return IoUtil.readAllChars(in);
      }
      catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    public StatusType getStatusType() { return getFirst(); }
    public String getResponseBody() { return getSecond(); }

    public String toString() {
      return getStatusType().getStatusCode() + " " + getStatusType().getReasonPhrase() + ": " + getResponseBody();
    }
  }

  public static <T> T getResponseObject(String urlPath, Class<T> responseObjectClass) {
    try {
      return new ObjectMapper().readerFor(responseObjectClass).readValue(new URL(Resources.SUBSETTING_SERVICE_URL + urlPath));
    }
    catch (IOException e) {
      throw new RuntimeException("Unable to read and reserialize studies endpoint response object", e);
    }
  }

  public static Either<InputStream,RequestFailure> makePostRequest(
      String url, Object postBodyObject, String expectedResponseType) throws JsonProcessingException {

    Response response =  ClientBuilder.newClient()
      .target(url)
      .request(expectedResponseType)
      .post(Entity.entity(serializeToJson(postBodyObject), MediaType.APPLICATION_JSON));

    return response.getStatusInfo().getFamily().equals(Status.Family.SUCCESSFUL) ?
        Either.left((InputStream)response.getEntity()) :
        Either.right(new RequestFailure(response));
  }

  private static String serializeToJson(Object object) throws JsonProcessingException {
    return new ObjectMapper().writerFor(object.getClass()).writeValueAsString(object);
  }
}
