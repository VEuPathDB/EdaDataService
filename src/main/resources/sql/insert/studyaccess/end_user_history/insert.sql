INSERT INTO
  studyaccess.end_user_history (
    end_user_id           -- 1, int64
  , user_id               -- 2, int64
  , dataset_presenter_id  -- 3, string
  , restriction_level_id  -- 4, int32
  , approval_status_id    -- 5, int32
  , start_date            -- 6, date
  , duration              -- 7, int64
  , purpose               -- 8, string
  , research_question     -- 9, string
  , analysis_plan         -- 10, string
  , dissemination_plan    -- 11, string
  , prior_auth            -- 12, string
  , denial_reason         -- 13, string
  , date_denied           -- 14, timestamp with time zone
  , allow_self_edits      -- 15, boolean
  , history_action        -- 16, string
  , history_cause_user    -- 17, int64
  )
VALUES
  (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)

