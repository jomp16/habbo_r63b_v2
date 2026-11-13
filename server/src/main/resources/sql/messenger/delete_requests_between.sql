DELETE
FROM `messenger_requests`
WHERE (`to_id` = :user_id AND `from_id` = :other_user_id)
   OR (`to_id` = :other_user_id AND `from_id` = :user_id)
