INSERT INTO
  `users_ips` (
    `user_id`,
    `ip`,
    `country_code`,
    `country`,
    `region_code`,
    `region`,
    `timezone`,
    `latitude`,
    `longitude`
  )
VALUES (
  :user_id,
  :ip,
  :country_code,
  :country,
  :region_code,
  :region,
  :timezone,
  :latitude,
  :longitude
)