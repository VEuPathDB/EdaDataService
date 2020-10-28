package org.veupathdb.service.edads.util;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import org.veupathdb.service.edads.generated.model.APIFilter;
import org.veupathdb.service.edads.generated.model.APIStudyDetail;
import org.veupathdb.service.edads.generated.model.DerivedVariable;
import org.veupathdb.service.edads.util.StreamSpec;

public class EdaClient {

  public static InputStream getDataStream(
      APIStudyDetail study,
      Optional<List<APIFilter>> subset,
      Optional<List<DerivedVariable>> derivedVariables,
      StreamSpec spec) {
    // TODO: open connection to subsetting (and later stream processing) service, request data, and return stream to response body
    return null;
  }
}
