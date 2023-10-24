SELECT
  dataset_id
, is_manager
FROM
  studyaccess.providers
WHERE
  user_id = ?
