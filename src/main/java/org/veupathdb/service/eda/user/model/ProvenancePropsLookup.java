package org.veupathdb.service.eda.user.model;

import jakarta.ws.rs.NotFoundException;
import org.veupathdb.service.eda.generated.model.AnalysisProvenance;
import org.veupathdb.service.eda.generated.model.AnalysisSummary;
import org.veupathdb.service.eda.generated.model.CurrentProvenanceProps;
import org.veupathdb.service.eda.generated.model.CurrentProvenancePropsImpl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProvenancePropsLookup {

  private static CurrentProvenanceProps toCurrentProps(String modTime, Boolean isPublic) {
    CurrentProvenanceProps currentState = new CurrentProvenancePropsImpl();
    currentState.setIsDeleted(modTime == null);
    currentState.setModificationTime(modTime);
    currentState.setIsPublic(isPublic);
    return currentState;
  }

  public static void assignCurrentProvenanceProps(UserDataFactory dataFactory, List<? extends AnalysisSummary> summaries) {
    Map<String, CurrentProvenanceProps> idToCurrentPropsCache = new HashMap<>();
    // add all mod dates in the current list to save lookups already done
    summaries.stream().forEach(a ->
        idToCurrentPropsCache.put(a.getAnalysisId(),
            toCurrentProps(a.getModificationTime(), a.getIsPublic())));
    summaries.stream().forEach(a -> {
      AnalysisProvenance prov = a.getProvenance();
      if (prov != null) {
        String parentId = prov.getOnImport().getAnalysisId();
        CurrentProvenanceProps currentProps = idToCurrentPropsCache.get(parentId);
        if (currentProps == null) {
          // still need to look up
          try {
            // try to find parent analysis (may have been deleted)
            AnalysisDetailWithUser parentAnalysis = dataFactory.getAnalysisById(parentId);
            currentProps = toCurrentProps(
                parentAnalysis.getModificationTime(),
                parentAnalysis.getIsPublic()
            );
          }
          catch (NotFoundException e) {
            // parent has been deleted
            currentProps = toCurrentProps(null, null);
          }
          idToCurrentPropsCache.put(parentId, currentProps);
        }
        prov.setCurrent(currentProps);
      }
    });
  }
}
