SELECT `ig`.`id`,
       `ig`.`item_name`,
       `ig`.`amount`,
       `ig`.`extradata`,
       (`il`.`id` is not null) as `is_limited`
FROM `items_gift` `ig`
         left join `items_limited` `il` on `il`.`item_id` = `ig`.`id`
WHERE `ig`.`item_id` = :item_id