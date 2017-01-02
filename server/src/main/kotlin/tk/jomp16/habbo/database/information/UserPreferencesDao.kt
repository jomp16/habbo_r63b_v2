/*
 * Copyright (C) 2017 jomp16
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

package tk.jomp16.habbo.database.information

import net.sf.ehcache.Ehcache
import net.sf.ehcache.Element
import tk.jomp16.habbo.HabboServer
import tk.jomp16.habbo.game.user.information.UserPreferences
import tk.jomp16.habbo.kotlin.addAndGetEhCache
import tk.jomp16.habbo.kotlin.insertWithIntGeneratedKey

object UserPreferencesDao {
    private val userPreferencesCache: Ehcache = HabboServer.cacheManager.addAndGetEhCache("userPreferencesCache")

    fun getUserPreferences(userId: Int, cache: Boolean = true): UserPreferences {
        if (!cache || !userPreferencesCache.isKeyInCache(userId)) {
            val userPreferences = HabboServer.database {
                select("SELECT * FROM users_preferences WHERE user_id = :user_id LIMIT 1",
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
                    insertWithIntGeneratedKey("INSERT INTO users_preferences (user_id) VALUES (:id)",
                            mapOf(
                                    "id" to userId
                            )
                    )
                }

                // Now fetch it again, doing a one recursive call, and returns this
                return getUserPreferences(userId, cache)
            }

            userPreferencesCache.put(Element(userId, userPreferences))
        }

        return userPreferencesCache.get(userId).objectValue as UserPreferences
    }

    fun savePreferences(userPreferences: UserPreferences) {
        HabboServer.database {
            update("UPDATE users_preferences SET volume = :volume, prefer_old_chat = :prefer_old_chat, " +
                    "ignore_room_invite = :ignore_room_invite, disable_camera_follow = :disable_camera_follow, " +
                    "navigator_x = :navigator_x, navigator_y = :navigator_y, navigator_width = :navigator_width, " +
                    "navigator_height = :navigator_height, hide_in_room = :hide_in_room, block_new_friends = :block_new_friends, " +
                    "chat_color = :chat_color, friend_bar_open = :friend_bar_open WHERE id = :id",
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