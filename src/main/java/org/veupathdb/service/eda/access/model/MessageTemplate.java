package org.veupathdb.service.eda.access.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;

public class MessageTemplate
{
  public static final String
    FIELD_SUBJECT = "subject",
    FIELD_BODY    = "body";

  private final String subject;
  private final String body;

  @JsonCreator
  public MessageTemplate(
    @JsonProperty(FIELD_SUBJECT) final String subject,
    @JsonProperty(FIELD_BODY)    final  String body
  ) {
    this.subject = subject;
    this.body    = body;
  }

  @JsonGetter(FIELD_SUBJECT)
  public String getSubject() {
    return subject;
  }

  @JsonGetter(FIELD_BODY)
  public String getBody() {
    return body;
  }
}
