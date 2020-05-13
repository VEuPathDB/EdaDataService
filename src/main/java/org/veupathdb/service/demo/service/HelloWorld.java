package org.veupathdb.service.demo.service;

import org.gusdb.fgputil.accountdb.UserProfile;

import javax.ws.rs.core.Context;

import java.util.Optional;
import java.util.Random;

import org.veupathdb.lib.container.jaxrs.middleware.AuthFilter.Authenticated;
import org.veupathdb.service.demo.generated.model.HelloPostRequest;
import org.veupathdb.service.demo.generated.model.HelloPostResponseImpl;
import org.veupathdb.service.demo.generated.model.HelloResponse.GreetingType;
import org.veupathdb.service.demo.generated.model.HelloResponseImpl;
import org.veupathdb.service.demo.generated.model.ServerErrorImpl;
import org.veupathdb.service.demo.generated.resources.Hello;

public class HelloWorld implements Hello {

  @Context
  private UserProfile user;

  @Override
  public GetHelloResponse getHello() {
    var out = new HelloResponseImpl();
    out.setGreeting(GreetingType.HELLOWORLD);
    return GetHelloResponse.respond200WithApplicationJson(out);
  }

  @Override
  @Authenticated
  public PostHelloResponse postHello(HelloPostRequest entity) {
    var rand = new Random();

    // Throw a 500 every once in a while for fun.
    if (rand.nextInt(4) == 2) {
      var out = new ServerErrorImpl();
      out.setMessage("Whoops!");
      return PostHelloResponse.respond500WithApplicationJson(out);
    }

    var out = new HelloPostResponseImpl();
    out.setMessage(String.format("Hello %s!", Optional.ofNullable(user)
      .map(UserProfile::getProperties)
      .map(p -> p.get("firstName"))
      .orElse("whut")));

    return PostHelloResponse.respond200WithApplicationJson(out);
  }
}
