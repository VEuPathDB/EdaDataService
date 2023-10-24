package org.veupathdb.service.eda.access.model;

public class DatasetEmails
{
  public final String to;
  public final String from;

  public DatasetEmails(final String to, final String from) {
    this.to   = to;
    this.from = from;
  }
}
