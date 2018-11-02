UPDATE
    `users_preferences`
SET `volume`                = :volume,
    `prefer_old_chat`       = :prefer_old_chat,
    `ignore_room_invite`    = :ignore_room_invite,
    `disable_camera_follow` = :disable_camera_follow,
    `navigator_x`           = :navigator_x,
    `navigator_y`           = :navigator_y,
    `navigator_width`       = :navigator_width,
    `navigator_height`      = :navigator_height,
    `hide_in_room`          = :hide_in_room,
    `block_new_friends`     = :block_new_friends,
    `chat_color`            = :chat_color,
    `friend_bar_open`       = :friend_bar_open
WHERE `id` = :id