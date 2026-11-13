UPDATE `users_stats`
SET `gifts_received` = `gifts_received` + 1
WHERE `user_id` = :user_id
