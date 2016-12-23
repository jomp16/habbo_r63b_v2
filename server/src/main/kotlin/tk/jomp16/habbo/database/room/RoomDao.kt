/*
 * Copyright (C) 2016 jomp16
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

package tk.jomp16.habbo.database.room

import com.github.andrewoma.kwery.core.Row
import tk.jomp16.habbo.HabboServer
import tk.jomp16.habbo.game.item.room.RoomItem
import tk.jomp16.habbo.game.room.RightData
import tk.jomp16.habbo.game.room.RoomData
import tk.jomp16.habbo.game.room.RoomState
import tk.jomp16.habbo.game.room.RoomType
import tk.jomp16.habbo.game.room.model.RoomModel
import tk.jomp16.habbo.kotlin.insertAndGetGeneratedKey
import tk.jomp16.habbo.util.Vector3

object RoomDao {
    fun getRoomsData() = HabboServer.database {
        select("SELECT * FROM rooms") {
            getRoomData(it)
        }
    }

    fun getRoomData(roomId: Int) = HabboServer.database {
        select("SELECT * FROM rooms WHERE id = :room_id",
                mapOf(
                        "room_id" to roomId
                )
        ) {
            getRoomData(it)
        }.first()
    }

    private fun getRoomData(row: Row) = RoomData(
            row.int("id"),
            RoomType.valueOf(row.string("room_type").toUpperCase()),
            row.string("caption"),
            row.int("owner_id"),
            row.string("description"),
            row.int("category"),
            RoomState.valueOf(row.string("state").toUpperCase()),
            row.int("trade_state"),
            row.int("users_max"),
            row.string("model_name"),
            row.int("score"),
            row.string("tags").split(","),
            row.string("password"),
            row.string("wallpaper"),
            row.string("floor"),
            row.string("landscape"),
            row.boolean("hide_wall"),
            row.int("wall_thick"),
            row.int("wall_height"),
            row.int("floor_thick"),
            row.int("mute_settings"),
            row.int("ban_settings"),
            row.int("kick_settings"),
            row.int("chat_type"),
            row.int("chat_balloon"),
            row.int("chat_speed"),
            row.int("chat_max_distance"),
            row.int("chat_flood_protection"),
            row.int("group_id"),
            row.boolean("allow_pets"),
            row.boolean("allow_pets_eat"),
            row.boolean("allow_walk_through")
    )

    fun getRoomModels() = HabboServer.database {
        select("SELECT * FROM rooms_models") {
            RoomModel(
                    it.string("id"),
                    0,
                    Vector3(
                            it.int("door_x"),
                            it.int("door_y"),
                            it.double("door_z")
                    ),
                    it.int("door_dir"),
                    it.string("heightmap").trim().split("[\\r\\n]+".toRegex()),
                    it.boolean("club_only")
            )
        }
    }

    fun getCustomRoomModels() = HabboServer.database {
        select("SELECT * FROM rooms_models_customs") {
            RoomModel(
                    it.string("id"),
                    it.int("room_id"),
                    Vector3(
                            it.int("door_x"),
                            it.int("door_y"),
                            it.double("door_z")
                    ),
                    it.int("door_dir"),
                    it.string("heightmap").trim().split("[\\r\\n]+".toRegex()),
                    false
            )
        }
    }

    fun getRights(roomId: Int) = HabboServer.database {
        select("SELECT id, user_id FROM rooms_rights WHERE room_id = :room_id",
                mapOf(
                        "room_id" to roomId
                )
        ) {
            RightData(
                    it.int("id"),
                    it.int("user_id")
            )
        }
    }

    fun getWordFilter(roomId: Int) = HabboServer.database {
        select("SELECT word FROM rooms_word_filter WHERE room_id = :room_id",
                mapOf(
                        "room_id" to roomId
                )
        ) {
            it.string("word")
        }
    }

    fun createRoom(userId: Int, name: String, description: String, model: String, category: Int, maxUsers: Int, tradeSettings: Int) = HabboServer.database {
        insertAndGetGeneratedKey(
                "INSERT INTO rooms (caption, description, owner_id, model_name, category, users_max, trade_state)" +
                        "VALUES (:caption, :description, :owner_id, :model_name, :category, :users_max, :trade_state)",
                mapOf(
                        "caption" to name,
                        "description" to description,
                        "owner_id" to userId,
                        "model_name" to model,
                        "category" to category,
                        "users_max" to maxUsers,
                        "trade_state" to tradeSettings.toString()
                )
        )
    }

    fun saveItems(roomId: Int, roomItemsToSave: List<RoomItem>) {
        HabboServer.database {
            batchUpdate(
                    "UPDATE items SET room_id = :room_id, x = :x, y = :y, z = :z, rot = :rot, wall_pos = :wall_pos, extra_data = :extra_data WHERE id = :id",
                    roomItemsToSave.map {
                        mapOf(
                                "room_id" to roomId,
                                "x" to it.position.x,
                                "y" to it.position.y,
                                "z" to it.position.z,
                                "rot" to it.rotation,
                                "wall_pos" to it.wallPosition,
                                "extra_data" to it.extraData,
                                "id" to it.id
                        )
                    }
            )

            // todo: dimmer
            // todo: wired
            // todo: teleport
        }
    }

    fun updateRoomData(roomData: RoomData) {
        HabboServer.database {
            update("UPDATE rooms SET caption = :caption, description = :description, state = :state, password = :password, " +
                    "users_max = :users_max, category = :category, tags = :tags, trade_state = :trade_state, allow_pets = :allow_pets, " +
                    "allow_pets_eat = :allow_pets_eat, allow_walk_through = :allow_walk_through, hide_wall = :hide_wall, " +
                    "wall_thick = :wall_thick, floor_thick = :floor_thick, wall_height = :wall_height, mute_settings = :mute_settings, " +
                    "kick_settings = :kick_settings, ban_settings = :ban_settings, chat_type = :chat_type, chat_balloon = :chat_balloon, " +
                    "chat_speed = :chat_speed, chat_max_distance = :chat_max_distance, chat_flood_protection = :chat_flood_protection, " +
                    "floor = :floor, wallpaper = :wallpaper, landscape = :landscape, group_id = :group_id WHERE id = :room_id",
                    mapOf(
                            "caption" to roomData.caption,
                            "description" to roomData.description,
                            "state" to roomData.state.name.toLowerCase(),
                            "password" to roomData.password,
                            "users_max" to roomData.usersMax,
                            "category" to roomData.category,
                            "tags" to roomData.tags.joinToString(","),
                            "trade_state" to roomData.tradeState.toString(),
                            "allow_pets" to roomData.allowPets,
                            "allow_pets_eat" to roomData.allowPetsEat,
                            "allow_walk_through" to roomData.allowWalkThrough,
                            "hide_wall" to roomData.hideWall,
                            "wall_thick" to roomData.wallThick,
                            "floor_thick" to roomData.floorThick,
                            "wall_height" to roomData.wallHeight,
                            "mute_settings" to roomData.muteSettings,
                            "kick_settings" to roomData.kickSettings,
                            "ban_settings" to roomData.banSettings,
                            "chat_type" to roomData.chatType,
                            "chat_balloon" to roomData.chatBalloon,
                            "chat_speed" to roomData.chatSpeed,
                            "chat_max_distance" to roomData.chatMaxDistance,
                            "chat_flood_protection" to roomData.chatFloodProtection,
                            "floor" to roomData.floor,
                            "wallpaper" to roomData.wallpaper,
                            "landscape" to roomData.landscape,
                            "group_id" to roomData.groupId,
                            "room_id" to roomData.id
                    )
            )
        }
    }

    fun addWordFilter(roomId: Int, wordFilter: String) = HabboServer.database {
        insertAndGetGeneratedKey("INSERT INTO rooms_word_filter (room_id, word) VALUES (:room_id, :word)",
                mapOf(
                        "room_id" to roomId,
                        "word" to wordFilter
                )
        )
    }

    fun removeWordFilter(roomId: Int, wordFilter: String) {
        HabboServer.database {
            update("DELETE FROM rooms_word_filter WHERE room_id = :room_id AND word = :word",
                    mapOf(
                            "room_id" to roomId,
                            "word" to wordFilter
                    )
            )
        }
    }
}