package org.veupathdb.service.demo.service;

import org.veupathdb.service.demo.generated.model.HelloPostBody;
import org.veupathdb.service.demo.generated.model.HelloPostResponseImpl;
import org.veupathdb.service.demo.generated.model.HelloResponse.GreetingType;
import org.veupathdb.service.demo.generated.model.HelloResponseImpl;
import org.veupathdb.service.demo.generated.model.ServerErrorResponse.StatusType;
import org.veupathdb.service.demo.generated.model.ServerErrorResponseImpl;
import org.veupathdb.service.demo.generated.resources.Hello;
import org.veupathdb.service.demo.middleware.AuthFilter.Authenticated;

import java.util.Random;

public class HelloWorld implements Hello {
  @Override
  public GetHelloResponse getHello() {
    var out = new HelloResponseImpl();
    out.setGreeting(GreetingType.HELLOWORLD);

    return GetHelloResponse.respond200WithApplicationJson(out);
  }

  @Override
  @Authenticated
  public PostHelloResponse postHello(String authKey, HelloPostBody entity) {
    var rand = new Random();

    // Throw a 500 every once in a while for fun.
    if (rand.nextInt(4) == 2) {
      var out = new ServerErrorResponseImpl();
      out.setStatus(StatusType.SERVERERROR);
      out.setMessage("Whoops!");
      return PostHelloResponse.respond500WithApplicationJson(out);
    }

    var out = new HelloPostResponseImpl();
    out.setMessage(String.format("Hello %s!", entity.getGreet()));
    return PostHelloResponse.respond200WithApplicationJson(out);
  }
}
