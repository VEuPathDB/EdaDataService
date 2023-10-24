package org.veupathdb.service.eda.access.model;

public class UserRow
{
  private long userId;
  private String email;
  private String firstName;
  private String lastName;
  private String organization;

  public long getUserId() {
    return userId;
  }

  public UserRow setUserId(long userId) {
    this.userId = userId;
    return this;
  }

  public String getEmail() {
    return email;
  }

  public UserRow setEmail(String email) {
    this.email = email;
    return this;
  }

  public String getFirstName() {
    return firstName;
  }

  public UserRow setFirstName(String firstName) {
    this.firstName = firstName;
    return this;
  }

  public String getLastName() {
    return lastName;
  }

  public UserRow setLastName(String lastName) {
    this.lastName = lastName;
    return this;
  }

  public String getOrganization() {
    return organization;
  }

  public UserRow setOrganization(String organization) {
    this.organization = organization;
    return this;
  }
}
