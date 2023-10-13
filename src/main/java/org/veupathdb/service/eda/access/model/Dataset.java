package org.veupathdb.service.eda.access.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.Logger;
import org.gusdb.fgputil.functional.Functions;
import org.veupathdb.lib.container.jaxrs.providers.LogProvider;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class Dataset
{
  private static final Logger log = LogProvider.logger(Dataset.class);
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private static final String
    ERR_BAD_PROP_NAME = "Unrecognized property '%s'.";

  public enum Property
  {
    // FROM address
    REQUEST_EMAIL("requestEmail"),
    // TO address
    REQUEST_EMAIL_BCC("requestEmailBcc"),
    REQUEST_EMAIL_BODY("requestEmailBody"),
    REQUEST_NEEDS_APPROVAL("requestNeedsApproval"),
    // Email template fields
    REQUEST_ACCESS_FIELDS("requestAccessFields"),
    CUSTOM_APPROVAL_EMAIL_BODY("customApprovalEmailBody"),
    DAYS_FOR_APPROVAL("daysForApproval"),
    REQUEST_EMAIL_BODY_REQUESTER("requestEmailBodyRequester"),
    REQUEST_EMAIL_BODY_MANAGER("requestEmailBodyManager");

    public final String dbName;

    Property(final String dbName) {
      this.dbName = dbName;
    }

    @JsonCreator
    public static Property fromDbName(final String dbName) {
      for (var e : values())
        if (e.dbName.equals(dbName))
          return e;

      throw new IllegalArgumentException(String.format(ERR_BAD_PROP_NAME, dbName));
    }
  }

  /**
   * <code>dataset_presenter_id VARCHAR2(15)</code>
   */
  private String datasetId;

  /**
   * <code>name VARCHAR2(200)</code>
   */
  private String name;

  /**
   * <code>dataset_name_pattern VARCHAR2(200)</code>
   */
  private String datasetNamePattern;

  /**
   * <code>display_name VARCHAR2(200)</code>
   */
  private String displayName;

  /**
   * <code>short_display_name VARCHAR2(200)</code>
   */
  private String shortDisplayName;

  /**
   * <code>short_attribution VARCHAR2(200)</code>
   */
  private String shortAttribution;

  /*
   * <code>summary CLOB</code>
   */
  // private String summary;

  /*
   * <code>protocol VARCHAR2(4000)</code>
   */
  // private String protocol;

  /*
   * <code>description CLOB</code>
   */
  // private String description;

  /*
   * <code>usage VARCHAR2(4000)</code>
   */
  // private String usage;

  /*
   * <code>caveat VARCHAR2(4000)</code>
   */
  // private String caveat;

  /*
   * <code>acknowledgement VARCHAR2(4000)</code>
   */
  // private String acknowledgement;

  /*
   * <code>release_policy VARCHAR2(4000)</code>
   */
  // private String releasePolicy;

  /**
   * <code>display_category VARCHAR2(60)</code>
   */
  private String displayCategory;

  /**
   * <code>type VARCHAR2(100)</code>
   */
  private String type;

  /**
   * <code>subtype VARCHAR2(100)</code>
   */
  private String subtype;

  /**
   * <code>category VARCHAR2(100)</code>
   */
  private String category;

  /**
   * <code>subtype NUMBER(1)</code>
   */
  private boolean isSpeciesScope;

  /**
   * <code>build_number_introduced NUMBER(5)</code>
   */
  private int buildNumberIntroduced;

  /**
   * <code>dataset_sha1_digest VARCHAR2(50)</code>
   */
  private String datasetSha1Digest;

  /**
   * <code>datasetproperty.property -> datasetproperty.value</code>
   */
  private final Map<Property, String> properties;

  public Dataset() {
    properties = new HashMap<>();
  }

  public String getDatasetId() {
    return datasetId;
  }

  public Dataset setDatasetId(String datasetId) {
    this.datasetId = datasetId;
    return this;
  }

  public String getName() {
    return name;
  }

  public Dataset setName(String name) {
    this.name = name;
    return this;
  }

  public String getDatasetNamePattern() {
    return datasetNamePattern;
  }

  public Dataset setDatasetNamePattern(String datasetNamePattern) {
    this.datasetNamePattern = datasetNamePattern;
    return this;
  }

  public String getDisplayName() {
    return displayName;
  }

  public Dataset setDisplayName(String displayName) {
    this.displayName = displayName;
    return this;
  }

  public String getShortDisplayName() {
    return shortDisplayName;
  }

  public Dataset setShortDisplayName(String shortDisplayName) {
    this.shortDisplayName = shortDisplayName;
    return this;
  }

  public String getShortAttribution() {
    return shortAttribution;
  }

  public Dataset setShortAttribution(String shortAttribution) {
    this.shortAttribution = shortAttribution;
    return this;
  }

  public String getDisplayCategory() {
    return displayCategory;
  }

  public Dataset setDisplayCategory(String displayCategory) {
    this.displayCategory = displayCategory;
    return this;
  }

  public String getType() {
    return type;
  }

  public Dataset setType(String type) {
    this.type = type;
    return this;
  }

  public String getSubtype() {
    return subtype;
  }

  public Dataset setSubtype(String subtype) {
    this.subtype = subtype;
    return this;
  }

  public String getCategory() {
    return category;
  }

  public Dataset setCategory(String category) {
    this.category = category;
    return this;
  }

  public boolean isSpeciesScope() {
    return isSpeciesScope;
  }

  public Dataset setSpeciesScope(boolean speciesScope) {
    isSpeciesScope = speciesScope;
    return this;
  }

  public int getBuildNumberIntroduced() {
    return buildNumberIntroduced;
  }

  public Dataset setBuildNumberIntroduced(int buildNumberIntroduced) {
    this.buildNumberIntroduced = buildNumberIntroduced;
    return this;
  }

  public String getDatasetSha1Digest() {
    return datasetSha1Digest;
  }

  public Dataset setDatasetSha1Digest(String datasetSha1Digest) {
    this.datasetSha1Digest = datasetSha1Digest;
    return this;
  }

  public Dataset putProperty(final Property key, final String value) {
    this.properties.put(key, value);
    return this;
  }

  public Dataset putProperties(final Map<Property, String> props) {
    this.properties.putAll(props);
    return this;
  }

  public Map<Property, String> getProperties() {
    return Collections.unmodifiableMap(properties);
  }

  public String getRequestEmailBcc() {
    return getProperties().get(Property.REQUEST_EMAIL_BCC);
  }

  public String getCustomApprovalEmailBody() {
    return getProperties().get(Property.CUSTOM_APPROVAL_EMAIL_BODY);
  }

  public String getRequestEmailBodyRequester() {
    return getProperties().get(Property.REQUEST_EMAIL_BODY_REQUESTER);
  }

  public String getRequestEmailBodyManager() {
    return getProperties().get(Property.REQUEST_EMAIL_BODY_MANAGER);
  }

  public String getDaysForApproval() {
    return getProperties().get(Property.DAYS_FOR_APPROVAL);
  }

  public String getPriorAuth() {
    return Optional.ofNullable(getProperties().get(Property.REQUEST_ACCESS_FIELDS))
        .map(Functions.fSwallow(OBJECT_MAPPER::readTree))
        .flatMap(tree -> Optional.ofNullable(tree.get("prior_auth")))
        .map(JsonNode::asText)
        .orElse(null);
  }
}
