package org.veupathdb.service.eda.access.service.permissions;

import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import org.glassfish.jersey.server.ContainerRequest;
import org.gusdb.fgputil.Wrapper;
import org.veupathdb.lib.container.jaxrs.model.User;
import org.veupathdb.service.eda.access.controller.Util;
import org.veupathdb.service.eda.generated.model.*;
import org.veupathdb.service.eda.access.model.ApprovalStatus;
import org.veupathdb.service.eda.access.model.DatasetProps;
import org.veupathdb.service.eda.access.service.dataset.DatasetRepo;
import org.veupathdb.service.eda.access.service.provider.ProviderRepo;
import org.veupathdb.service.eda.access.service.staff.StaffRepo;
import org.veupathdb.service.eda.access.service.user.EndUserRepo;
import org.veupathdb.service.eda.access.service.user.EndUserUtil;
import org.veupathdb.service.eda.access.service.userdataset.UserDatasetIsaStudies;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class PermissionService
{
  private static PermissionService instance;

  public PermissionsGetResponse getUserPermissions(ContainerRequest request) {
    return getUserPermissions(Util.requireUser(request));
  }

  public StudyPermissionInfo getUserPermissions(ContainerRequest request, String datasetId) {
    try {
      User user = Util.requireUser(request);
      // get map of all datasets this user knows about (clunky but gets the job done)
      PermissionMap knownDatasets = (PermissionMap)getUserPermissions(user).getPerDataset();

      // find the one for this study if it exists
      Optional<StudyPermissionInfo> studyPermission = knownDatasets.entrySet().stream()
          .filter(entry -> entry.getKey().equals(datasetId))
          .findAny()
          // if found, convert for return
          .map(entry -> {
            StudyPermissionInfo info = new StudyPermissionInfoImpl();
            info.setDatasetId(entry.getKey());
            info.setStudyId(entry.getValue().getStudyId());
            info.setIsUserStudy(entry.getValue().getIsUserStudy());
            info.setActionAuthorization(entry.getValue().getActionAuthorization());
            return info;
          });

      if (studyPermission.isPresent()) return studyPermission.get();

      // otherwise, user does not have study visibility but want to see if it's a user study
      return UserDatasetIsaStudies.getUserStudyByDatasetId(datasetId).orElseThrow(
          () -> new NotFoundException("No study exists with dataset ID: " + datasetId)
      );
    }
    catch (WebApplicationException e) {
      throw e;
    }
    catch (Exception e) {
      throw new InternalServerErrorException(e);
    }
  }

  public PermissionsGetResponse getUserPermissions(User user) {
    var out = new PermissionsGetResponseImpl();

    try {
      Wrapper<Boolean> grantAll = new Wrapper<>(false);

      // check if current user is staff and set outgoing perms accordingly
      StaffRepo.Select.byUserId(user.getUserID())
        .ifPresent(s -> {
          // all staff get access to all studies
          grantAll.set(true);
          // assign staff role
          out.setIsStaff(true);
          if (s.isOwner()) {
            // staff/owner is essentially a superuser
            out.setIsOwner(true);
          }
        });

      // level of access assigned to each dataset
      var datasetProps = DatasetRepo.Select.getDatasetProps();

      // if datasetId is present, then user is provider; boolean indicates isManager
      Map<String,Boolean> providerInfoMap = ProviderRepo.Select.datasets(user.getUserID());

      // list of datasetIds user has approved access for
      Map<String, ApprovalStatus> approvalStatusMap = EndUserRepo.Select.datasets(user.getUserID());

      // assign specific permissions on each dataset for this user
      PermissionMap datasetPerms = getPermissionMap(grantAll.get(), datasetProps, providerInfoMap, approvalStatusMap);

      // supplement official studies with studies from user datasets
      datasetPerms.putAll(UserDatasetIsaStudies.getUserDatasetPermissions(user.getUserID()));

      // set permission map on permissions object
      out.setPerDataset(datasetPerms);

      return out;
    }
    catch (WebApplicationException e) {
      throw e;
    }
    catch (Exception e) {
      throw new InternalServerErrorException(e);
    }
  }

  private static PermissionMap getPermissionMap(boolean grantToAllDatasets,
      List<DatasetProps> datasetProps,
      Map<String, Boolean> providerInfoMap,
      Map<String, ApprovalStatus> approvalStatusMap) {
    var permissionMap = new PermissionMap();
    for (DatasetProps dataset : datasetProps) {

      DatasetPermissionEntry permEntry = new DatasetPermissionEntryImpl();

      permEntry.setStudyId(dataset.studyId);
      permEntry.setSha1Hash(dataset.sha1hash);
      permEntry.setDisplayName(dataset.displayName);
      permEntry.setShortDisplayName(dataset.shortDisplayName);
      permEntry.setDescription(dataset.description);
      permEntry.setIsUserStudy(false);

      boolean isProvider = providerInfoMap.containsKey(dataset.datasetId);

      // set permission type for this dataset
      permEntry.setType(isProvider ?
          DatasetPermissionLevel.PROVIDER :
          DatasetPermissionLevel.ENDUSER);

      // is manager if isProvider and provider info map has value true
      permEntry.setIsManager(isProvider && providerInfoMap.get(dataset.datasetId));

      ApprovalStatus requestStatus = approvalStatusMap.get(dataset.datasetId);
      permEntry.setAccessRequestStatus(EndUserUtil.convertApproval(requestStatus));

      boolean accessGranted = requestStatus == ApprovalStatus.APPROVED;
      boolean grantAllPermsForThisDataset = grantToAllDatasets || isProvider || accessGranted;

      ActionList actions = new ActionListImpl();

      // all users have access to the study page of all studies
      actions.setStudyMetadata(true);

      // controls search, visualizations, small results
      boolean allowBasicAccess = grantAllPermsForThisDataset || dataset.accessLevel.allowsBasicAccess();
      actions.setSubsetting(allowBasicAccess);
      actions.setVisualizations(allowBasicAccess);
      actions.setResultsFirstPage(allowBasicAccess);

      // controls access to full dataset, downloads
      boolean allowFullAccess = grantAllPermsForThisDataset || dataset.accessLevel.allowsFullAccess();
      actions.setResultsAll(allowFullAccess);

      permEntry.setActionAuthorization(actions);

      // add to map
      if (permissionMap.containsKey(dataset.datasetId)) {
        throw new IllegalStateException("Database (datasetpresenter table or studyiddatasetid table) " +
            "contains more than one row for dataset ID " + dataset.datasetId);
      }
      permissionMap.put(dataset.datasetId, permEntry);
    }

    return permissionMap;
  }

  public static PermissionService getInstance() {
    if (instance == null)
      instance = new PermissionService();

    return instance;
  }
}
