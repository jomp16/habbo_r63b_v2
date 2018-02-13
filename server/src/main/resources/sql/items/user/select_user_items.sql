SELECT
  `id`,
  `user_id`,
  `item_name`,
  `extra_data`
FROM
  `items`
WHERE
  `room_id` IS NULL
  AND
  `user_id` = :user_id