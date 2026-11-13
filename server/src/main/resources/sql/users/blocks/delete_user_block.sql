DELETE FROM `users_blocks`
WHERE `user_id` = :user_id
  AND `blocked_user_id` = :blocked_user_id
