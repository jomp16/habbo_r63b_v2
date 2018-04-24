INSERT INTO
  `users_ips` (
    `user_id`,
    `ip`,
    `ip_type`,
    `internal`,
    `continent_code`,
    `continent`,
    `country_code`,
    `country`,
    `region_code`,
    `region`,
    `latitude`,
    `longitude`
  )
VALUES (
  :user_id,
  :ip,
  :ip_type,
  :internal,
  :continent_code,
  :continent,
  :country_code,
  :country,
  :region_code,
  :region,
  :latitude,
  :longitude
)