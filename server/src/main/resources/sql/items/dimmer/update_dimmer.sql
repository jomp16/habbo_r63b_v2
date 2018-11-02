UPDATE
    `items_dimmer`
SET `enabled`        = :enabled,
    `current_preset` = :current_preset,
    `preset_one`     = :preset_one,
    `preset_two`     = :preset_two,
    `preset_three`   = :preset_three
WHERE `id` = :id