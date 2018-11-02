SELECT `id`,
       `room_id`,
       `item_name`,
       `extra_data`,
       `x`,
       `y`,
       `z`,
       `rot`,
       `wall_pos`,
       `user_id`
FROM `items`
WHERE `room_id` = :room_id
ORDER BY `id` DESC