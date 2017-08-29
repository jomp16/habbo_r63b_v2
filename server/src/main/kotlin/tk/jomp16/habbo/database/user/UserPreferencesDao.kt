/*
 * Copyright (C) 2015-2017 jomp16
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

package tk.jomp16.habbo.database.user

import tk.jomp16.habbo.HabboServer
import tk.jomp16.habbo.game.user.information.UserPreferences
import tk.jomp16.habbo.kotlin.insertWithIntGeneratedKey

object UserPreferencesDao {
    fun getUserPreferences(userId: Int): UserPreferences {
        val userPreferences = HabboServer.database {
            select(javaClass.classLoader.getResource("sql/users/preferences/select_user_preferences.sql").readText(),
                    mapOf(
                            "user_id" to userId
                    )
            ) {
                UserPreferences(
                        it.int("id"),
                        it.string("volume"),
                        it.boolean("prefer_old_chat"),
                        it.boolean("ignore_room_invite"),
                        it.boolean("disable_camera_follow"),
                        it.int("navigator_x"),
                        it.int("navigator_y"),
                        it.int("navigator_width"),
                        it.int("navigator_height"),
                        it.boolean("hide_in_room"),
                        it.boolean("block_new_friends"),
                        it.int("chat_color"),
                        it.boolean("friend_bar_open")
                )
            }.firstOrNull()
        }

        if (userPreferences == null) {
            // no users preferences, create it
            HabboServer.database {
                insertWithIntGeneratedKey(javaClass.classLoader.getResource("sql/users/preferences/insert_user_preferences.sql").readText(),
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
        HabboServer.database {
            update(javaClass.classLoader.getResource("sql/users/preferences/update_user_preferences.sql").readText(),
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
                            "id" to userPreferences.id
                    )
            )
        }
    }
}