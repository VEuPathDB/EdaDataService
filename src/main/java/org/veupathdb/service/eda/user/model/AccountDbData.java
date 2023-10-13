package org.veupathdb.service.eda.user.model;

import org.gusdb.fgputil.Tuples.TwoTuple;
import org.gusdb.fgputil.accountdb.AccountManager;
import org.gusdb.fgputil.accountdb.UserProfile;
import org.gusdb.fgputil.accountdb.UserPropertyName;
import org.veupathdb.lib.container.jaxrs.utils.db.DbManager;
import org.veupathdb.service.eda.generated.model.AnalysisSummaryWithUser;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class AccountDbData {

  public static class AccountDataPair extends TwoTuple<String,String> {
    public AccountDataPair(Map<String,String> props) {
      super(
        getDisplayName(props),
        Optional.ofNullable(props.get("organization")).orElse("")
      );
    }

    public AccountDataPair(long userId) {
      super("User_" + userId, "");
    }

    public String getName() { return getFirst(); }
    public String getOrganization() { return getSecond(); }
  }

  private static final List<UserPropertyName> USER_PROPERTIES = Arrays.asList(new UserPropertyName[] {
    new UserPropertyName("firstName", "first_name", true),
    new UserPropertyName("middleName", "middle_name", false),
    new UserPropertyName("lastName", "last_name", true),
    new UserPropertyName("organization", "organization", true),
  });

  private final AccountManager _acctDb = new AccountManager(DbManager.accountDatabase(), "useraccounts.", USER_PROPERTIES);
  private final Map<Long, AccountDataPair> _accountDataCache = new HashMap<>();

  public List<AnalysisSummaryWithUser> populateOwnerData(List<AnalysisSummaryWithUser> analyses) {
    return analyses.stream()
      .peek(analysis -> {
        long userId = analysis.getUserId().longValue();
        AccountDataPair accountData = _accountDataCache.get(userId);
        if (accountData == null) {
          accountData = getUserDataById(userId);
          _accountDataCache.put(userId, accountData);
        }
        analysis.setUserName(accountData.getName());
        analysis.setUserOrganization(accountData.getOrganization());
      })
      .collect(Collectors.toList());
  }

  public AccountDataPair getUserDataById(long userId) {
    UserProfile profile = _acctDb.getUserProfile(userId);
    return profile == null || profile.isGuest()
        // use id-derived name for guests and deleted(?) users
        ? new AccountDataPair(userId)
        // use values in user's profile
        : new AccountDataPair(profile.getProperties());
  }

  /**
   * Copy of the method in the EuPathDB OAuth impl that performs this task
   *
   * @param userProperties set of user properties
   * @return displayable name value that combines first/middle/last name values
   */
  private static String getDisplayName(Map<String,String> userProperties) {
    String firstName = userProperties.get("firstName");
    String middleName = userProperties.get("middleName");
    String lastName = userProperties.get("lastName");
    String name = null;
    if (firstName != null && !firstName.isEmpty()) name = firstName;
    if (middleName != null && !middleName.isEmpty()) {
      name = (name == null ? middleName : name + " " + middleName);
    }
    if (lastName != null && !lastName.isEmpty()) {
      name = (name == null ? lastName : name + " " + lastName);
    }
    return name;
  }
}
