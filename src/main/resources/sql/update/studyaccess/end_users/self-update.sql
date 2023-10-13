UPDATE
  studyaccess.end_users
SET
  purpose = ?            -- 1, string
, research_question = ?  -- 2, string
, analysis_plan = ?      -- 3, string
, dissemination_plan = ? -- 4, string
, prior_auth = ?         -- 5, string
, allow_self_edits = ?   -- 6, boolean
, approval_status_id = ? -- 7, short
WHERE
  user_id = ?                  -- 7, long
  AND dataset_presenter_id = ? -- 8, string
