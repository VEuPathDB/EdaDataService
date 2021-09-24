package org.veupathdb.service.eda.ds.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.WebApplicationException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gusdb.fgputil.IoUtil;
import org.gusdb.fgputil.functional.FunctionalInterfaces;
import org.gusdb.fgputil.functional.FunctionalInterfaces.ConsumerWithException;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RFileInputStream;
import org.rosuda.REngine.Rserve.RFileOutputStream;
import org.rosuda.REngine.Rserve.RserveException;
import org.veupathdb.service.eda.ds.Resources;
import org.veupathdb.service.eda.ds.util.RFileSetProcessor.RFileProcessingSpec;

public class RServeClient {

  private static final Logger LOG = LogManager.getLogger(RServeClient.class);

  public static void useRConnection(ConsumerWithException<RConnection> consumer) {
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
      throw e instanceof WebApplicationException ? (WebApplicationException)e :
          new RuntimeException("Unable to complete processing", e);
    }
    finally {
      if (c != null) {
        c.close();
      }
    }
  }

  public static void useRConnectionWithRemoteFiles(Map<String, InputStream> dataStreams, ConsumerWithException<RConnection> consumer) {
    useRConnectionWithProcessedRemoteFiles(new RFileSetProcessor(dataStreams), consumer);
  }

  public static void useRConnectionWithProcessedRemoteFiles(RFileSetProcessor filesProcessor, ConsumerWithException<RConnection> consumer) {
    useRConnection(connection -> {
      List<String> filesTooBig = new ArrayList<>();
      try {
        for (RFileProcessingSpec spec : filesProcessor) {
          LOG.info("Transferring data stream '" + spec.name + "' to RServe");
          RFileOutputStream dataset = connection.createFile(spec.name);
          IoUtil.transferStream(dataset, spec.stream);
          dataset.close();

          // have R read the file using the assigned reader (may be a no-op)
          spec.fileReader.accept(connection);

          // if requested, check number of rows read against max and throw later if violated
          spec.maxAllowedRows.ifPresent(maxRows -> checkMaxRows(connection, spec.name, filesTooBig, maxRows));
        }

        // if any files too big, throw
        if (!filesTooBig.isEmpty()) {
          LOG.info("Found the following results too large to process: " + String.join(", ", filesTooBig));
          throw new BadRequestException("Result is too large for this visualization to display.");
        }
        // all files written and (possibly) validated; consumer may now use them in its RServe call
        LOG.info("All data streams transferred.");
        consumer.accept(connection);
      }
      finally {
        for (RFileProcessingSpec spec : filesProcessor) {
          connection.removeFile(spec.name);
        }
      }
    });
  }

  private static void checkMaxRows(RConnection connection, String name, List<String> filesTooBig, Integer maxRows) {
    try {
      int numRows = connection.eval("nrow("+ name + ")").asInteger();
      LOG.info("R found " + numRows + " rows in file " + name);
      if (numRows - 1 /* header? */ > maxRows) {
        filesTooBig.add(name);
      }
    }
    catch (Exception e) {
      LOG.error("Error checking num rows; ignoring", e);
    }
  }

  public static void streamResult(RConnection connection, String cmd, OutputStream out)
      throws RserveException, REXPMismatchException, IOException {
    String outFile = connection.eval(cmd).asString();
    try (RFileInputStream response = connection.openFile(outFile)) {
      IoUtil.transferStream(out, response);
    }
    out.flush();
  }
}
