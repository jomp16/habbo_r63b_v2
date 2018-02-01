UPDATE
  `users_stats`
SET
  `last_online`              = :last_online,
  `credits_last_update`      = :credits_last_update,
  `favorite_group`           = :favorite_group,
  `online_seconds`           = :online_seconds,
  `respect`                  = :respect,
  `daily_respect_points`     = :daily_respect_points,
  `daily_pet_respect_points` = :daily_pet_respect_points,
  `respect_last_update`      = :respect_last_update,
  `marketplace_tickets`      = :marketplace_tickets,
  `room_visits`              = :room_visits
WHERE
  `id` = :id