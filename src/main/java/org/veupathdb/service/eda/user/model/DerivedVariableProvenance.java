package org.veupathdb.service.eda.user.model;

import org.json.JSONObject;
import org.veupathdb.service.eda.generated.model.DerivedVariableProvenanceImpl;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Objects;

import static org.gusdb.fgputil.functional.Functions.also;

public class DerivedVariableProvenance {
  private final OffsetDateTime copyDate;
  private final String copiedFrom;

  public DerivedVariableProvenance(OffsetDateTime copyDate, String copiedFrom) {
    this.copyDate = Objects.requireNonNull(copyDate);
    this.copiedFrom = Objects.requireNonNull(copiedFrom);
  }

  public DerivedVariableProvenance(Date copyDate, String copiedFrom) {
    this(OffsetDateTime.ofInstant(copyDate.toInstant(), ZoneId.systemDefault()), copiedFrom);
  }

  public DerivedVariableProvenance(org.veupathdb.service.eda.generated.model.DerivedVariableProvenance provenance) {
    this(provenance.getCopyDate(), provenance.getCopiedFrom());
  }

  public DerivedVariableProvenance(JSONObject json) {
    this(
      OffsetDateTime.parse(json.getString("copyDate"), DateTimeFormatter.ISO_DATE_TIME),
      json.getString("copiedFrom")
    );
  }

  public OffsetDateTime getCopyDate() {
    return copyDate;
  }

  public String getCopiedFrom() {
    return copiedFrom;
  }

  public org.veupathdb.service.eda.generated.model.DerivedVariableProvenance toAPIType() {
    return also(new DerivedVariableProvenanceImpl(), it -> {
      it.setCopyDate(Date.from(copyDate.toInstant()));
      it.setCopiedFrom(copiedFrom);
    });
  }

  public JSONObject toJSONObject() {
    return new JSONObject()
      .put("copyDate", copyDate.format(DateTimeFormatter.ISO_DATE_TIME))
      .put("copiedFrom", copiedFrom);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof DerivedVariableProvenance that)) return false;
    return Objects.equals(copyDate, that.copyDate) && Objects.equals(copiedFrom, that.copiedFrom);
  }

  @Override
  public int hashCode() {
    return Objects.hash(copyDate, copiedFrom);
  }
}
