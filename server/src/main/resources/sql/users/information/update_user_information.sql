UPDATE
  `users`
SET
  `online`      = :online,
  `ip_last`     = :ip_last,
  `credits`     = :credits,
  `pixels`      = :pixels,
  `vip_points`  = :vip_points,
  `figure`      = :figure,
  `gender`      = :gender,
  `motto`       = :motto,
  `home_room`   = :home_room
WHERE
  `id` = :id