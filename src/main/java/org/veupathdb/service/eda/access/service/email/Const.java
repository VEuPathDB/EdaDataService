package org.veupathdb.service.eda.access.service.email;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.veupathdb.service.eda.access.model.MessageTemplate;

class Const
{
  private static final String CONFIG_FILE = "/email.yaml";

  public static final String
    FIELD_PROVIDER  = "provider-register",
    FIELD_END_USER  = "end-user-register",
    FIELD_EDIT_NOTE = "edit-notification",
    FIELD_APPROVE   = "approve-notification",
    FIELD_DENY      = "deny-notification";

  public static final MessageTemplate ProviderTemplate;

  public static final MessageTemplate EndUserTemplate;

  public static final MessageTemplate EditNotification;

  public static final MessageTemplate ApproveNotification;

  public static final MessageTemplate DenyNotification;

  static {
    try {
      var config = new YAMLMapper()
        .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
        .readValue(Const.class.getResourceAsStream(CONFIG_FILE), Temp.class);

      ProviderTemplate = config.provider;
      EndUserTemplate  = config.endUser;
      EditNotification = config.edits;
      ApproveNotification = config.approve;
      DenyNotification = config.deny;
    } catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static class Temp {
    @JsonProperty(FIELD_PROVIDER)
    public MessageTemplate provider;

    @JsonProperty(FIELD_END_USER)
    public MessageTemplate endUser;

    @JsonProperty(FIELD_EDIT_NOTE)
    public MessageTemplate edits;

    @JsonProperty(FIELD_APPROVE)
    public MessageTemplate approve;

    @JsonProperty(FIELD_DENY)
    public MessageTemplate deny;

  }
}
