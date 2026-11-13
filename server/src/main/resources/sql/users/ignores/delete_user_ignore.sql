DELETE FROM `users_ignores`
WHERE `user_id` = :user_id
  AND `ignored_user_id` = :ignored_user_id
