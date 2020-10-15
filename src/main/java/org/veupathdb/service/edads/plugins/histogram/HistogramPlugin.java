package org.veupathdb.service.edads.plugins.histogram;

import java.io.IOException;
import java.io.OutputStream;

import org.gusdb.fgputil.validation.ValidationBundle;
import org.veupathdb.service.edads.core.EdadsPlugin;
import org.veupathdb.service.edads.generated.model.HistogramPostRequest;

public class HistogramPlugin extends EdadsPlugin<HistogramPostRequest>{

  @Override
  protected ValidationBundle validateRequest(HistogramPostRequest request) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  protected void writeResults(OutputStream out) throws IOException {
    // TODO Auto-generated method stub
    
  }

}
