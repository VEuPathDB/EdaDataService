SELECT
  s.staff_id,
  s.is_owner,
  a.email,
  a.user_id,
  (
    SELECT value
    FROM useraccounts.account_properties
    WHERE user_id = s.user_id
    AND key = 'first_name'
  ) first_name,
  (
    SELECT value
    FROM useraccounts.account_properties
    WHERE user_id = s.user_id
    AND key = 'last_name'
  ) last_name
, (
    SELECT value
    FROM useraccounts.account_properties
    WHERE user_id = s.user_id
    AND key = 'organization'
  ) organization
FROM
  studyaccess.staff s
  INNER JOIN useraccounts.accounts a
    ON s.user_id = a.user_id
WHERE
  s.user_id = ?
