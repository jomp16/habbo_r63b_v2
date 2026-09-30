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

package ovh.rwx.habbo.database.user

import ovh.rwx.habbo.game.user.information.UserPreferences
import ovh.rwx.habbo.database.db

object UserPreferencesDao {
    fun getUserPreferences(userId: Int): UserPreferences {
        val userPreferences = db {
            queryOne<UserPreferences>(
                "sql/users/preferences/select_user_preferences.sql",
                mapOf("user_id" to userId)
            )
        }

        if (userPreferences == null) {
            // no users preferences, create it
            db {
                insertWithIntGeneratedKey(
                    javaClass.classLoader.getResource("sql/users/preferences/insert_user_preferences.sql").readText(),
                    mapOf(
                        "id" to userId
                    )
                )
            }
            // Now fetch it again, doing a one recursive call, and returns this
            return getUserPreferences(userId)
        }

        return userPreferences
    }

    fun savePreferences(userPreferences: UserPreferences) {
        db {
            update(
                javaClass.classLoader.getResource("sql/users/preferences/update_user_preferences.sql").readText(),
                mapOf(
                    "volume" to userPreferences.volume,
                    "prefer_old_chat" to userPreferences.preferOldChat,
                    "ignore_room_invite" to userPreferences.ignoreRoomInvite,
                    "disable_camera_follow" to userPreferences.disableCameraFollow,
                    "navigator_x" to userPreferences.navigatorX,
                    "navigator_y" to userPreferences.navigatorY,
                    "navigator_width" to userPreferences.navigatorWidth,
                    "navigator_height" to userPreferences.navigatorHeight,
                    "hide_in_room" to userPreferences.hideInRoom,
                    "block_new_friends" to userPreferences.blockNewFriends,
                    "chat_color" to userPreferences.chatColor,
                    "friend_bar_open" to userPreferences.friendBarOpen,
                    "friend_stream_enabled" to userPreferences.friendStreamEnabled,
                    "id" to userPreferences.id
                )
            )
        }
    }
}

