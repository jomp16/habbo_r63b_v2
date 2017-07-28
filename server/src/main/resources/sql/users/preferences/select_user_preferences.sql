SELECT
  *
FROM
  `users_preferences`
WHERE
  `user_id` = :user_id
LIMIT 1