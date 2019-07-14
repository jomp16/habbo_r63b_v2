SELECT `id`, `user_two_id` AS `user_id`, `relationship`
FROM `messenger_friendships`
WHERE `user_one_id` = :user_one_id