package org.veupathdb.service.edads.plugin;

import java.io.IOException;
import java.io.OutputStream;
import java.util.function.Consumer;

import org.gusdb.fgputil.validation.ValidationBundle;
import org.veupathdb.service.edads.generated.model.APIStudyDetail;
import org.veupathdb.service.edads.generated.model.BaseAnalysisConfig;
import org.veupathdb.service.edads.service.StudiesService;

public abstract class AbstractEdadsPlugin<T extends BaseAnalysisConfig> implements Consumer<OutputStream> {

  protected T _request;

  protected abstract ValidationBundle validateRequest(T request);

  protected abstract void writeResults(OutputStream out) throws IOException;

  public AbstractEdadsPlugin<T> processRequest(T request) {
    _request = request;
    APIStudyDetail study = StudiesService.getStudy(_request.getStudy());
    _request.getDerivedVariables();
    _request.getSubset();
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
