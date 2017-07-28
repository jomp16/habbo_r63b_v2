UPDATE
  `items_wired`
SET
  `delay`     = :delay,
  `items`     = :items,
  `message`   = :message,
  `options`   = :options1,
  `extradata` = :extradata
WHERE
  `id` = :id