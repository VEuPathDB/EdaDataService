package org.veupathdb.service.edads.plugin.scatterplot;

import java.io.IOException;
import java.io.OutputStream;

import org.gusdb.fgputil.validation.ValidationBundle;
import org.veupathdb.service.edads.generated.model.ScatterplotPostRequest;
import org.veupathdb.service.edads.plugin.AbstractEdadsPlugin;

public class ScatterplotPlugin extends AbstractEdadsPlugin<ScatterplotPostRequest> {

  @Override
  protected ValidationBundle validateRequest(ScatterplotPostRequest request) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  protected void writeResults(OutputStream out) throws IOException {
    // TODO Auto-generated method stub
    
  }

}
