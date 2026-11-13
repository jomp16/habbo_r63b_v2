INSERT INTO `users_blocks` (`user_id`, `blocked_user_id`)
VALUES (:user_id, :blocked_user_id)
ON DUPLICATE KEY UPDATE `user_id` = `user_id`
