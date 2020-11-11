package org.veupathdb.service.edads.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.Response.StatusType;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.grizzly.http.util.Header;
import org.gusdb.fgputil.FormatUtil;
import org.gusdb.fgputil.IoUtil;
import org.gusdb.fgputil.Tuples.TwoTuple;
import org.gusdb.fgputil.functional.Either;
import org.veupathdb.service.edads.Resources;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class NetworkUtils {

  private static Logger LOG = LogManager.getLogger(NetworkUtils.class);

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

    String json = serializeToJson(postBodyObject);
    LOG.info("Will send following POST request to " + url + "\n" + json);

    MultivaluedMap<String,Object> headers = new MultivaluedHashMap<>();
    headers.add(Header.Accept.toString(), "*/*");
    if (Resources.logResponseHeadersAsClient()) {
      headers.add("X-Jersey-Tracing-Accept", "any");
    }

    Response response = ClientBuilder.newClient()
      .target(url)
      .request(expectedResponseType)
      .headers(headers)
      .post(Entity.entity(json, MediaType.APPLICATION_JSON));

    if (Resources.logResponseHeadersAsClient())
      logHeaders(response.getHeaders());

    return response.getStatusInfo().getFamily().equals(Status.Family.SUCCESSFUL) ?
        Either.left((InputStream)response.getEntity()) :
        Either.right(new RequestFailure(response));
  }

  private static void logHeaders(MultivaluedMap<String, Object> headers) {
    List<String> headerNames = new ArrayList<>(headers.keySet());
    Collections.sort(headerNames);
    for (String header : headerNames) {
      LOG.info("Header " + header + ": " + FormatUtil.join(headers.get(header).toArray(), ","));
    }
  }

  private static String serializeToJson(Object object) throws JsonProcessingException {
    return new ObjectMapper().writerFor(object.getClass()).writeValueAsString(object);
  }
}
