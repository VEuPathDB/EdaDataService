select
  u.dataset_presenter_id,
  s.name as status
from
  studyaccess.end_users u,
  studyaccess.approval_status s
where
  u.approval_status_id = s.approval_status_id
and
  user_id = ?
