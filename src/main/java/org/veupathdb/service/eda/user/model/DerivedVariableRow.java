package org.veupathdb.service.eda.user.model;

import com.fasterxml.jackson.databind.JsonNode;
import org.veupathdb.service.eda.generated.model.DerivedVariableGetResponse;
import org.veupathdb.service.eda.generated.model.DerivedVariableGetResponseImpl;
import org.veupathdb.service.eda.generated.model.DerivedVariablePostRequest;
import org.veupathdb.service.eda.user.Utils;

import java.time.OffsetDateTime;
import java.util.Objects;

import static org.gusdb.fgputil.functional.Functions.also;
import static org.veupathdb.service.eda.user.Utils.mapIfPresent;

/**
 * Represents a complete database row for a Derived Variable.
 */
public class DerivedVariableRow {

  public static final int MAX_DISPLAY_NAME_LENGTH = 256;
  public static final int MAX_DESCRIPTION_LENGTH = 4000;

  private final String variableID;
  private final long userID;
  private final String datasetID;
  private final String entityID;
  private final String displayName;
  private final String description;
  private final DerivedVariableProvenance provenance;
  private final String functionName;
  private final JsonNode config;

  public DerivedVariableRow(
    String variableID,
    long userID,
    String datasetID,
    String entityID,
    String displayName,
    String description,
    DerivedVariableProvenance provenance,
    String functionName,
    JsonNode config
  ) {
    this.variableID   = Objects.requireNonNull(variableID);
    this.userID       = userID;
    this.datasetID    = Objects.requireNonNull(datasetID);
    this.entityID     = Objects.requireNonNull(entityID);
    this.displayName  = Objects.requireNonNull(displayName);
    this.description  = description;
    this.provenance   = provenance;
    this.functionName = Objects.requireNonNull(functionName);
    this.config       = Objects.requireNonNull(config);
  }

  public DerivedVariableRow(String variableID, long userID, DerivedVariablePostRequest request) {
    this(
      variableID,
      userID,
      request.getDatasetId(),
      request.getEntityId(),
      request.getDisplayName(),
      request.getDescription(),
      null,
      request.getFunctionName(),
      Utils.JSON.convertValue(Objects.requireNonNull(request.getConfig()), JsonNode.class)
    );
  }

  public DerivedVariableRow(String variableID, long userID, DerivedVariableRow copyFrom) {
    this(
      variableID,
      userID,
      copyFrom.datasetID,
      copyFrom.entityID,
      copyFrom.displayName,
      copyFrom.description,
      copyFrom.provenance != null ? copyFrom.provenance : new DerivedVariableProvenance(OffsetDateTime.now(), copyFrom.variableID),
      copyFrom.functionName,
      copyFrom.config
    );
  }

  public String getVariableID() {
    return variableID;
  }

  public long getUserID() {
    return userID;
  }

  public String getDatasetID() {
    return datasetID;
  }

  public String getEntityID() {
    return entityID;
  }

  public String getDisplayName() {
    return displayName;
  }

  public String getDescription() {
    return description;
  }

  public DerivedVariableProvenance getProvenance() {
    return provenance;
  }

  public String getFunctionName() {
    return functionName;
  }

  public JsonNode getConfig() {
    return config;
  }

  public DerivedVariableGetResponse toGetResponse() {
    return also(new DerivedVariableGetResponseImpl(), out -> {
      out.setVariableId(variableID);
      // out.setUserId(userID);
      out.setDatasetId(datasetID);
      out.setEntityId(entityID);
      out.setDisplayName(displayName);
      out.setDescription(description);
      out.setProvenance(mapIfPresent(provenance, DerivedVariableProvenance::toAPIType));
      out.setFunctionName(functionName);
      out.setConfig(config);
    });
  }
}
