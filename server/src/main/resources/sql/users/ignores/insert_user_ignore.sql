INSERT INTO `users_ignores` (`user_id`, `ignored_user_id`)
VALUES (:user_id, :ignored_user_id)
ON DUPLICATE KEY UPDATE `user_id` = `user_id`
