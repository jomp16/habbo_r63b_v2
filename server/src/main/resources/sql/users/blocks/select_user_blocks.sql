SELECT `blocked_user_id`
FROM `users_blocks`
WHERE `user_id` = :user_id
ORDER BY `created_at` ASC, `blocked_user_id` ASC
