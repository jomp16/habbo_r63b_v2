SELECT `i`.`id`,
       `i`.`user_id`,
       `i`.`item_name`,
       `i`.`extra_data`,
       `i`.`is_builders_club`,
       (`il`.`id` is not null) as `is_limited`
FROM `items_gift_contents` `gc`
         INNER JOIN `items` `i` ON `i`.`id` = `gc`.`item_id`
         left join `items_limited` `il` on `il`.`item_id` = `i`.`id`
WHERE `gc`.`gift_item_id` = :gift_item_id
ORDER BY `gc`.`item_id`
