SELECT 0 AS `id`, `id` AS `user_id`, 0 AS `relationship`
FROM `users`
WHERE `username` LIKE :username
  AND NOT `id` = :user_id
LIMIT 50