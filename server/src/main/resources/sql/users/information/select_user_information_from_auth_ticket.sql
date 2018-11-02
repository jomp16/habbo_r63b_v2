SELECT `id`
FROM `users`
WHERE `auth_ticket` = :ticket
LIMIT 1