UPDATE `rooms_models_customs`
SET `door_x`    = :door_x,
    `door_y`    = :door_y,
    `door_z`    = :door_z,
    `door_dir`  = :door_dir,
    `heightmap` = :heightmap
WHERE `id` = :id