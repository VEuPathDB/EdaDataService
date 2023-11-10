package org.veupathdb.service.eda.ds.utils;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class CommonFormats {
  public static final DateTimeFormatter TABULAR_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

  public long parseTabularDateToMillisSinceEpoch(String date) {
    return LocalDate.parse(date, CommonFormats.TABULAR_DATE_FORMAT)
        .atStartOfDay()
        .toInstant(ZoneOffset.UTC)
        .toEpochMilli();
  }
}
