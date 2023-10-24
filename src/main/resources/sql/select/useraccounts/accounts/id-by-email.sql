SELECT
  user_id
FROM
  useraccounts.accounts
WHERE
  LOWER(email) = LOWER(?)
