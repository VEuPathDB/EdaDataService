INSERT INTO
  studyaccess.end_users (
    user_id               -- 1, int64
  , dataset_presenter_id  -- 2, string
  , restriction_level_id  -- 3, int32
  , approval_status_id    -- 4, int32
  , start_date            -- 5, date
  , duration              -- 6, int64
  , purpose               -- 7, string
  , research_question     -- 8, string
  , analysis_plan         -- 9, string
  , dissemination_plan    -- 10, string
  , prior_auth            -- 11, string
  , denial_reason         -- 12, string
  , date_denied           -- 13, timestamp with time zone
  , allow_self_edits      -- 14, boolean
  )
VALUES
  (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
