package org.veupathdb.service.edads.plugin.histogram;

import java.io.IOException;
import java.io.OutputStream;

import org.gusdb.fgputil.validation.ValidationBundle;
import org.veupathdb.service.edads.generated.model.HistogramPostRequest;
import org.veupathdb.service.edads.plugin.AbstractEdadsPlugin;

public class HistogramPlugin extends AbstractEdadsPlugin<HistogramPostRequest>{

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
