SELECT `i`.`id`,
       `i`.`user_id`,
       `i`.`item_name`,
       `i`.`extra_data`,
       (`il`.`id` is not null) as `is_limited`
FROM `items` `i`
         left join `items_limited` `il` on `i`.`id` = `il`.`item_id`
WHERE `i`.`room_id` IS NULL
  AND `i`.`user_id` = :user_id