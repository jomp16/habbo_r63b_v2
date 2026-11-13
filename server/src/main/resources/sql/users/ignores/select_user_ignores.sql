SELECT `users_ignores`.`ignored_user_id` AS `user_id`, `users`.`username` AS `username`
FROM `users_ignores`
    INNER JOIN `users` ON `users`.`id` = `users_ignores`.`ignored_user_id`
WHERE `users_ignores`.`user_id` = :user_id
ORDER BY `users_ignores`.`created_at` ASC, `users_ignores`.`ignored_user_id` ASC
