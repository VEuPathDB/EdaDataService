package org.veupathdb.service.edads.core;

import java.io.IOException;
import java.io.OutputStream;
import java.util.function.Consumer;

import org.gusdb.fgputil.validation.ValidationBundle;

public abstract class EdadsPlugin<T> implements Consumer<OutputStream> {

  protected T _request;

  protected abstract ValidationBundle validateRequest(T request);

  protected abstract void writeResults(OutputStream out) throws IOException;

  public EdadsPlugin<T> processRequest(T request) {
    _request = request;
    validateRequest(_request);
    return this;
  }

  @Override
  public void accept(OutputStream out) {
    try {
      writeResults(out);
    }
    catch (IOException e) {
      throw new RuntimeException("Unable to stream results", e);
    }
  }
}
