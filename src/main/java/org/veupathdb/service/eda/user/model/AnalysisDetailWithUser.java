package org.veupathdb.service.eda.user.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.gusdb.fgputil.functional.Functions;
import org.veupathdb.service.eda.generated.model.AnalysisBase;
import org.veupathdb.service.eda.generated.model.AnalysisDescriptor;
import org.veupathdb.service.eda.generated.model.AnalysisDetailImpl;
import org.veupathdb.service.eda.generated.model.AnalysisListPostRequest;
import org.veupathdb.service.eda.generated.model.AnalysisListPostResponse;
import org.veupathdb.service.eda.generated.model.AnalysisListPostResponseImpl;
import org.veupathdb.service.eda.generated.model.AnalysisProvenance;
import org.veupathdb.service.eda.generated.model.AnalysisProvenanceImpl;
import org.veupathdb.service.eda.generated.model.OnImportProvenanceProps;
import org.veupathdb.service.eda.generated.model.OnImportProvenancePropsImpl;
import org.veupathdb.service.eda.user.Utils;
import org.veupathdb.service.eda.user.model.AccountDbData.AccountDataPair;

import java.sql.ResultSet;
import java.sql.SQLException;

import static org.veupathdb.service.eda.user.Utils.checkMaxSize;
import static org.veupathdb.service.eda.user.Utils.checkNonEmpty;

/**
 * Non-API analysis data container; used to pass information about a single
 * analysis (detail) back and forth between the service and data factory
 */
public class AnalysisDetailWithUser extends AnalysisDetailImpl {

  private long _userId;

  public AnalysisDetailWithUser(ResultSet rs) throws SQLException {
    UserDataFactory.populateDetailFields(this, rs);
  }

  public AnalysisDetailWithUser(String newAnalysisId, long ownerId, AnalysisListPostRequest request) {
    setInitializationFields(newAnalysisId, ownerId);
    setBaseFields(request);
    setDescriptor(request.getDescriptor());
    setNotes(request.getNotes());
    setIsPublic(request.getIsPublic());
  }

  public AnalysisDetailWithUser(String newAnalysisId, long ownerId, AnalysisDetailWithUser source, AccountDataPair provenanceOwner) {
    setInitializationFields(newAnalysisId, ownerId);
    setBaseFields(source);
    setDescriptor(source.getDescriptor());
    setNotes(source.getNotes());
    setIsPublic(false);
    setProvenance(createProvenance(source, provenanceOwner));
  }

  private static AnalysisProvenance createProvenance(AnalysisDetailWithUser source, AccountDataPair provenanceOwner) {
    AnalysisProvenance provenance = new AnalysisProvenanceImpl();
    OnImportProvenanceProps importProps = new OnImportProvenancePropsImpl();
    importProps.setAnalysisId(source.getAnalysisId());
    importProps.setAnalysisName(source.getDisplayName());
    importProps.setOwnerId(source.getUserId());
    importProps.setOwnerName(provenanceOwner.getName());
    importProps.setOwnerOrganization(provenanceOwner.getOrganization());
    importProps.setCreationTime(source.getCreationTime());
    importProps.setModificationTime(source.getModificationTime());
    importProps.setIsPublic(source.getIsPublic());
    provenance.setOnImport(importProps);
    return provenance;
  }

  private void setInitializationFields(String newAnalysisId, long ownerId) {
    String now = Utils.getCurrentDateTimeString();
    setUserId(ownerId);
    setCreationTime(now);
    setModificationTime(now);
    setAnalysisId(newAnalysisId);
  }

  private void setBaseFields(AnalysisBase base) {
    setDisplayName(checkMaxSize(50, "displayName", checkNonEmpty("displayName", base.getDisplayName())));
    setDescription(checkMaxSize(4000, "description", base.getDescription()));
    setStudyId(checkMaxSize(50, "studyId", checkNonEmpty("studyId", base.getStudyId())));
    setStudyVersion(checkMaxSize(50, "studyVersion", base.getStudyVersion())); // TODO: will eventually need to be non-empty
    setApiVersion(checkMaxSize(50, "apiVersion", base.getApiVersion()));       // TODO: will eventually need to be non-empty
  }

  public void setDescriptor(AnalysisDescriptor descriptor) {
    super.setDescriptor(descriptor);
    setNumFilters(descriptor.getSubset().getDescriptor().size());
    setNumComputations(descriptor.getComputations().size());
    setNumVisualizations(Functions.reduce(descriptor.getComputations(),
        (count, next) -> count + next.getVisualizations().size(), 0));
  }

  @JsonIgnore
  public long getUserId() {
    return _userId;
  }

  public void setUserId(long userId) {
    _userId = userId;
  }

  @JsonIgnore
  public AnalysisListPostResponse getIdObject() {
    AnalysisListPostResponse idObj = new AnalysisListPostResponseImpl();
    idObj.setAnalysisId(getAnalysisId());
    return idObj;
  }
}
