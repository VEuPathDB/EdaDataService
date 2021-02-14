package org.veupathdb.service.eda.ds.util;

import java.io.InputStream;
import java.net.URL;
import java.util.Map;
import org.gusdb.fgputil.IoUtil;
import org.gusdb.fgputil.functional.FunctionalInterfaces;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RFileOutputStream;
import org.veupathdb.service.eda.ds.Resources;

public class RServeClient {

  public static void useRConnection(FunctionalInterfaces.ConsumerWithException<RConnection> consumer) {
    RConnection c = null;
    try {
      String rServeUrlStr = Resources.RSERVE_URL;
      URL rServeUrl = new URL(rServeUrlStr);
      c = new RConnection(rServeUrl.getHost(), rServeUrl.getPort());
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
          RFileOutputStream dataset = connection.createFile(stream.getKey());
          IoUtil.transferStream(dataset, stream.getValue());
          dataset.close();
        }
        // all files written; consumer may now use them in its RServe call
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
