SELECT
  dataset_presenter_id
, name
, dataset_name_pattern
, display_name
, short_display_name
, short_attribution
, display_category
, type
, subtype
, category
, is_species_scope
, build_number_introduced
, dataset_sha1_digest
, (
    SELECT
      JSON_OBJECTAGG(property VALUE value)
    FROM
      apidbtuning.datasetproperty
    WHERE
      dataset_presenter_id = d.dataset_presenter_id
      AND property IN ('requestEmail', 'requestEmailBcc', 'requestEmailBodyRequester', 'requestEmailBodyManager', 'daysForApproval', 'requestAccessFields')
  ) AS properties
FROM
  apidbtuning.datasetpresenter d
WHERE
  d.dataset_presenter_id = ?
