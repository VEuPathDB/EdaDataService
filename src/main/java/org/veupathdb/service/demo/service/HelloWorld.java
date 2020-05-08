package org.veupathdb.service.demo.service;

import org.veupathdb.service.demo.model.HelloResponseImpl;
import org.veupathdb.service.demo.resources.Hello;

public class HelloWorld implements Hello {
  @Override
  public GetHelloResponse getHello() {
    var out = new HelloResponseImpl();
    out.setGreeting("Goodbye World");

    return GetHelloResponse.respond200WithApplicationJson(out);
  }
}
