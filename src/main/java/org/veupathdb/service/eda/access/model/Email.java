package org.veupathdb.service.eda.access.model;

import java.util.Objects;

public class Email
{
  private String[] to;

  private String[] cc;

  private String[] bcc;

  private String from;

  private String subject;

  private String body;

  public Email() {
    var empty = new String[0];

    to      = empty;
    cc      = empty;
    bcc     = empty;
    from    = "";
    subject = "";
    body    = "";
  }

  public String[] getTo() {
    return to;
  }

  public Email setTo(String[] to) {
    this.to = Objects.requireNonNull(to);
    return this;
  }

  public String[] getCc() {
    return cc;
  }

  public Email setCc(String[] cc) {
    this.cc = Objects.requireNonNull(cc);
    return this;
  }

  public String[] getBcc() {
    return bcc;
  }

  public Email setBcc(String[] bcc) {
    this.bcc = Objects.requireNonNull(bcc);
    return this;
  }

  public String getFrom() {
    return from;
  }

  public Email setFrom(String from) {
    this.from = Objects.requireNonNull(from);
    return this;
  }

  public String getSubject() {
    return subject;
  }

  public Email setSubject(String subject) {
    this.subject = Objects.requireNonNull(subject);
    return this;
  }

  public String getBody() {
    return body;
  }

  public Email setBody(String body) {
    this.body = Objects.requireNonNull(body);
    return this;
  }
}
