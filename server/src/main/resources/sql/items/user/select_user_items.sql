SELECT
  `id`,
  `user_id`,
  `item_name`,
  `extra_data`
FROM
  `items`
WHERE
  `room_id` = 0
  AND
  `user_id` = :user_id