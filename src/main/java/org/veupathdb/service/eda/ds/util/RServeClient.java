package org.veupathdb.service.eda.ds.util;

import java.io.InputStream;
import java.net.URL;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gusdb.fgputil.IoUtil;
import org.gusdb.fgputil.functional.FunctionalInterfaces;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RFileOutputStream;
import org.veupathdb.service.eda.ds.Resources;

public class RServeClient {

  private static final Logger LOG = LogManager.getLogger(RServeClient.class);

  public static void useRConnection(FunctionalInterfaces.ConsumerWithException<RConnection> consumer) {
    RConnection c = null;
    try {
      String rServeUrlStr = Resources.RSERVE_URL;
      URL rServeUrl = new URL(rServeUrlStr);
      LOG.info("Connecting to RServe at " + rServeUrlStr);
      c = new RConnection(rServeUrl.getHost(), rServeUrl.getPort());
      LOG.info("Connection established");
      consumer.accept(c);
    }
    catch (Exception e) {
      throw new RuntimeException("Unable to complete processing", e);
    }
    finally {
      if (c != null) {
        c.close();
      }
    }
  }

  public static void useRConnectionWithRemoteFiles(Map<String, InputStream> dataStreams, FunctionalInterfaces.ConsumerWithException<RConnection> consumer) {
    useRConnection(connection -> {
      try {
        for (Map.Entry<String, InputStream> stream : dataStreams.entrySet()) {
          LOG.info("Transferring data stream '" + stream.getKey() + "' to RServe");
          RFileOutputStream dataset = connection.createFile(stream.getKey());
          IoUtil.transferStream(dataset, stream.getValue());
          dataset.close();
        }
        // all files written; consumer may now use them in its RServe call
        LOG.info("All data streams transferred.");
        consumer.accept(connection);
      }
      finally {
        for (String name : dataStreams.keySet()) {
          connection.removeFile(name);
        }
      }
    });
  }
}
