package org.veupathdb.service.demo.container.health;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.Socket;

public class Pinger {
  private static final Logger LOG = LogManager.getLogger(Pinger.class);

  public static boolean isReachable(String addr, int port) {
    LOG.info("Pinging {}:{}", addr, port);

    try (var sock = new Socket(addr, port)) {
      sock.setSoTimeout(3000);
      sock.setTcpNoDelay(true);
      sock.getOutputStream().flush();
      return true;
    } catch (IOException e) {
      LOG.info("Ping failed for {}:{}", addr, port);
      LOG.debug(e);
      return false;
    }
  }
}
