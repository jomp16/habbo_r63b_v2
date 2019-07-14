DELETE
FROM `messenger_friendships`
WHERE `user_one_id` = :user_one_id
  AND `user_two_id` = :user_two_id