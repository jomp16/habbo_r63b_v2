SELECT COUNT(*) > 0 AS `unique_id_exists`
FROM `users_unique_ids`
WHERE `unique_id` = :unique_id
  AND `user_id` = :user_id