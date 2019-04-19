SELECT `i`.`id`,
       `i`.`room_id`,
       `i`.`item_name`,
       `i`.`extra_data`,
       `i`.`x`,
       `i`.`y`,
       `i`.`z`,
       `i`.`rot`,
       `i`.`wall_pos`,
       `i`.`user_id`,
       (`il`.`id` is not null) as `is_limited`
FROM `items` `i`
         left join `items_limited` `il` on `i`.`id` = `il`.`item_id`
WHERE `i`.`room_id` = :room_id
ORDER BY `i`.`id` DESC