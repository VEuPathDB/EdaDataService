package org.veupathdb.service.edads.plugins.recordcount;

import java.io.IOException;
import java.io.OutputStream;

import org.gusdb.fgputil.validation.ValidationBundle;
import org.veupathdb.service.edads.core.EdadsPlugin;
import org.veupathdb.service.edads.generated.model.RecordCountPostRequest;

public class RecordCountPlugin extends EdadsPlugin<RecordCountPostRequest> {

  @Override
  protected ValidationBundle validateRequest(RecordCountPostRequest request) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  protected void writeResults(OutputStream out) throws IOException {
    // TODO Auto-generated method stub
    
  }

}
