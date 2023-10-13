package org.veupathdb.service.eda.access.controller;

import jakarta.ws.rs.InternalServerErrorException;
import org.glassfish.jersey.server.ContainerRequest;
import org.veupathdb.lib.container.jaxrs.model.User;
import org.veupathdb.lib.container.jaxrs.providers.UserProvider;

public class Util
{
  @SuppressWarnings("FieldMayBeFinal")
  private static Util instance = new Util();

  Util() {}

  public static Util getInstance() {
    return instance;
  }

  public User mustGetUser(final ContainerRequest req) {
    return UserProvider.lookupUser(req)
      .orElseThrow(InternalServerErrorException::new);
  }

  public static User requireUser(final ContainerRequest req) {
    return getInstance().mustGetUser(req);
  }
}
