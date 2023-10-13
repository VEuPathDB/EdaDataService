UPDATE
  studyaccess.end_users
SET
  start_date = ?               -- 1, date
, duration = ?                 -- 2, long
, purpose = ?                  -- 3, string
, research_question = ?        -- 4, string
, analysis_plan = ?            -- 5, string
, dissemination_plan = ?       -- 6, string
, prior_auth = ?               -- 7, string
, restriction_level_id = ?     -- 8, short
, approval_status_id = ?       -- 9, short
, denial_reason = ?            -- 10, string
, date_denied = ?              -- 11, timestamp
, allow_self_edits = ?         -- 12, boolean
WHERE
  user_id = ?                  -- 13, long
  AND dataset_presenter_id = ? -- 14, string
