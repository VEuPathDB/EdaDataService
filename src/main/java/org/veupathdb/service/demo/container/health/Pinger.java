package org.veupathdb.service.demo.container.health;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.Socket;

public class Pinger {

  private final Logger log = LogManager.getLogger(Pinger.class);

  public boolean isReachable(String addr, int port) {
    log.debug("Pinging {}:{}", addr, port);

    try (var sock = new Socket(addr, port)) {
      sock.setSoTimeout(3000);
      sock.setTcpNoDelay(true);
      sock.getOutputStream().flush();
      return true;
    } catch (IOException e) {
      log.info("Ping failed for {}:{}", addr, port);
      log.debug(e);
      return false;
    }
  }
}
