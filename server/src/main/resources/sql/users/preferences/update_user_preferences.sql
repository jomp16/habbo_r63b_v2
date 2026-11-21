/*
 * Copyright (C) 2015-2026 jomp16 <root@rwx.ovh>
 *
 * This file is part of habbo_r63b_v2.
 *
 * habbo_r63b_v2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * habbo_r63b_v2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with habbo_r63b_v2. If not, see <http://www.gnu.org/licenses/>.
 */

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
    `ui_flags`              = :ui_flags,
    `friend_stream_enabled` = :friend_stream_enabled
WHERE `id` = :id