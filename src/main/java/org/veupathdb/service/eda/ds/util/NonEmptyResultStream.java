package org.veupathdb.service.eda.ds.util;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.ws.rs.BadRequestException;

/**
 * Wrapper around an input stream whose content must be a newline-delimited
 * set of records, including a header row.  This stream will transfer all
 * the passed input stream's data through its read() methods, with one
 * addition: after reading the header row (and optional newline), if no
 * more data is present, a JAX-RS BadRequestException will be thrown,
 * ending processing.
 */
public class NonEmptyResultStream extends BufferedInputStream {

  private final String _streamName;

  // initial state
  private boolean _foundFirstNewline = false;
  private boolean _continueChecking = true;

  public NonEmptyResultStream(String streamName, InputStream in) {
    super(in);
    _streamName = streamName;
  }

  @Override
  public int read() throws IOException {
    int nextByte = super.read();
    if (_continueChecking) {
      if (nextByte == -1) throwException();
      byte b = ((Integer)nextByte).byteValue();
      check(b);
    }
    return nextByte;
  }

  @Override
  public synchronized int read(byte b[], int off, int len) throws IOException {
    int bytesRead = super.read(b, off, len);
    if (_continueChecking) {
      if (bytesRead == -1) throwException();
      for (int i = 0; i < bytesRead && _continueChecking; i++) {
        check(b[i]);
      }
    }
    return bytesRead;
  }

  private void throwException() {
    throw new BadRequestException("Requested data stream '" + _streamName + "' did not contain any data.");
  }

  private void check(byte nextByte) {
    if (_foundFirstNewline) {
      _continueChecking = false; // done!
    }
    else if (nextByte == '\n') {
      _foundFirstNewline = true;
    }
  }
}
