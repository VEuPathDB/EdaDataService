package org.veupathdb.service.eda.user.model;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Defines an interface to write formatted columnar data.
 */
public interface TabularDataWriter {

  void write(String... column) throws IOException;

  void nextRecord() throws IOException;

  class TsvFormatter implements TabularDataWriter {
    private static final byte[] TAB_BYTES = "\t".getBytes(StandardCharsets.UTF_8);
    private static final byte[] NEW_LINE_BYTES = System.lineSeparator().getBytes(StandardCharsets.UTF_8);

    private boolean firstColumnInRow = true;
    private OutputStream outputStream;

    /**
     * Writes formatted TSV data to an output stream. Note that this takes an output stream instead of a writer for
     * compatibility with writing to a ZipOutputStream.
     */
    public TsvFormatter(OutputStream outputStream) {
      this.outputStream = outputStream;
    }

    @Override
    public void write(String... columns) throws IOException {
      for (String col: columns) {
        if (!firstColumnInRow) {
          outputStream.write(TAB_BYTES);
        }
        outputStream.write(col.getBytes(StandardCharsets.UTF_8));
        firstColumnInRow = false;
      }
    }

    @Override
    public void nextRecord() throws IOException {
      firstColumnInRow = true;
      outputStream.write(NEW_LINE_BYTES);
    }
  }
}
