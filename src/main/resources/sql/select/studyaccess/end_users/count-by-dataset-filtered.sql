SELECT
  COUNT(1)
FROM
  studyaccess.end_users v
WHERE
  v.dataset_presenter_id = ?
  AND v.approval_status_id = ?
