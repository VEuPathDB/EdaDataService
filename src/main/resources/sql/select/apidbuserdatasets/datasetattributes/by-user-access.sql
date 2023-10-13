SELECT
  studyds.dataset_stable_id,
  studyds.study_stable_id,
  studyds.name,
  studyds.description,
  dataset.is_owner
FROM (
    SELECT user_dataset_id, 1 as is_owner
    FROM apidbuserdatasets.userdatasetowner
    WHERE user_id = ?
    UNION
    SELECT user_dataset_id, 0 as is_owner
    FROM apidbuserdatasets.userdatasetsharedwith
    WHERE recipient_user_id = ?
) dataset, apidbuserdatasets.datasetattributes studyds
WHERE
  studyds.user_dataset_id = dataset.user_dataset_id
