SELECT
  pres.dataset_presenter_id,
  pres.dataset_sha1_digest,
  study.study_stable_id,
  prop.study_access,
  prop.display_name,
  prop.short_display_name,
  prop.description,
  prop.days_for_approval,
  prop.custom_approval_email_body
FROM
  apidbtuning.datasetpresenter pres,
  apidbtuning.studyiddatasetid study,
  (
    SELECT
      dataset_presenter_id,
      MAX(DECODE(property, 'studyAccess', value)) AS study_access,
      MAX(DECODE(property, 'datasetDisplayName', value)) AS display_name,
      MAX(DECODE(property, 'datasetShortDisplayName', value)) AS short_display_name,
      MAX(DECODE(property, 'datasetDescrip', value)) AS description,
      MAX(DECODE(property, 'daysForApproval', value)) AS days_for_approval,
      MAX(DECODE(property, 'customApprovalEmailBody', value)) AS custom_approval_email_body
    FROM apidbtuning.datasetproperty
    GROUP BY dataset_presenter_id
  ) prop
WHERE
  pres.dataset_presenter_id = study.dataset_id
AND
  pres.dataset_presenter_id = prop.dataset_presenter_id
