UPDATE
    `items_wired`
SET `delay`     = :delay,
    `items`     = :items,
    `message`   = :message,
    `options`   = :options,
    `extradata` = :extradata
WHERE `id` = :id