package org.veupathdb.service.eda.merge.core.request;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gusdb.fgputil.json.JsonUtil;
import org.gusdb.fgputil.validation.ValidationException;
import org.veupathdb.service.eda.Resources;
import org.veupathdb.service.eda.common.client.EdaSubsettingClient;
import org.veupathdb.service.eda.common.model.ReferenceMetadata;
import org.veupathdb.service.eda.generated.model.APIStudyDetail;
import org.veupathdb.service.eda.generated.model.DerivedVariableBulkMetadataRequest;
import org.veupathdb.service.eda.generated.model.DerivedVariableSpec;
import org.veupathdb.service.eda.merge.core.derivedvars.DerivedVariable;
import org.veupathdb.service.eda.merge.core.derivedvars.DerivedVariableFactory;

import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import static org.gusdb.fgputil.FormatUtil.NL;

/**
 * Processes and validates incoming request data for tabular and derived variable metadata requests.  This class is
 * responsible for providing a ReferenceMetadata which includes derived variables generated from the passed derived
 * variable specs.  It also provides a DerivedVariableFactory from which plugin instances are delivered.
 *
 * Note the superclass MergedTabularRequestResources, which supplements this class with target entity, subset filters,
 * and compute information needed for tabular requests.
 */
public class RequestResources {

  private static final Logger LOG = LogManager.getLogger(RequestResources.class);

  protected final EdaSubsettingClient _subsetSvc;
  protected final ReferenceMetadata _metadata;
  protected final List<DerivedVariableSpec> _derivedVariableSpecs;
  protected final DerivedVariableFactory _derivedVariableFactory;

  public RequestResources(DerivedVariableBulkMetadataRequest request, Entry<String, String> authHeader) throws ValidationException {

    LOG.info("Received merging post request: " + JsonUtil.serializeObject(request));

    // create subsetting service client
    _subsetSvc = new EdaSubsettingClient(Resources.SUBSETTING_SERVICE_URL, authHeader);

    // get raw metadata for requested study
    APIStudyDetail studyDetail = _subsetSvc.getStudy(request.getStudyId())
        .orElseThrow(() -> new ValidationException("No study found with ID " + request.getStudyId()));

    // create reference metadata using collected information
    _metadata = new ReferenceMetadata(studyDetail);
    _derivedVariableSpecs = request.getDerivedVariables();
    _derivedVariableFactory = new DerivedVariableFactory(_metadata, _derivedVariableSpecs);
    List<DerivedVariable> orderedDerivedVars = _derivedVariableFactory.getAllDerivedVars();
    LOG.debug("Will validate and incorporate derived vars in the following order: " + NL + orderedDerivedVars.stream().map(DerivedVariable::getColumnName).collect(Collectors.joining(NL)));
    for (DerivedVariable derivedVar : orderedDerivedVars) {
      LOG.debug("Validating depended vars of " + derivedVar.getColumnName() + " of type " + derivedVar.getFunctionName());
      // this call lets the plugins do additional setup where they can assume depended var metadata is incorporated
      derivedVar.validateDependedVariables();
      // incorporate this derived variable
      _metadata.incorporateDerivedVariable(derivedVar);
    }
  }

  public ReferenceMetadata getMetadata() { return _metadata; }
  public List<DerivedVariableSpec> getDerivedVariableSpecs() { return _derivedVariableSpecs; }
  public DerivedVariableFactory getDerivedVariableFactory() { return _derivedVariableFactory; }

}
