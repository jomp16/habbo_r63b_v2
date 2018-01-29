SELECT
  COUNT(*) > 0 AS `ip_exists`
FROM
  `users_ips`
WHERE
  `ip` = :ip AND `user_id` = :user_id