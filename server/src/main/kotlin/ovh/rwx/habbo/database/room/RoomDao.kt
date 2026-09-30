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

package ovh.rwx.habbo.database.room

import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.game.item.room.RoomItem
import ovh.rwx.habbo.game.room.RightData
import ovh.rwx.habbo.game.room.RoomData
import ovh.rwx.habbo.game.room.RoomState
import ovh.rwx.habbo.game.room.RoomType
import ovh.rwx.habbo.game.room.model.RoomModel
import ovh.rwx.habbo.database.sequence.HiLoSequence
import ovh.rwx.habbo.database.writebehind.WriteBehindManager
import ovh.rwx.habbo.util.Vector3
import ovh.rwx.habbo.database.*
import java.util.Locale
import org.slf4j.LoggerFactory

object RoomDao {
    private val log = LoggerFactory.getLogger(javaClass)
    val roomSequence = HiLoSequence("rooms", blockSize = 100)
    fun getRoomsData(): List<RoomData> = db {
        query<RoomDataDto>("SELECT * FROM `rooms`").map { it.toDomain() }
    }

    fun getRoomData(roomId: Int): RoomData = db {
        queryOne<RoomDataDto>(
            "SELECT * FROM `rooms` WHERE `id` = :room_id",
            mapOf("room_id" to roomId)
        )!!.toDomain()
    }

    fun getRoomModels(): List<RoomModel> = db {
        query<RoomModelDto>("SELECT * FROM `rooms_models`").map { it.toDomain() }
    }

    fun getCustomRoomModels(): List<RoomModel> = db {
        query<RoomModelDto>("SELECT * FROM `rooms_models_customs`").map { it.toDomain() }
    }

    fun getRights(roomId: Int): List<RightData> = db {
        query<RightData>(
            "SELECT `id`, `user_id` FROM `rooms_rights` WHERE `room_id` = :room_id",
            mapOf("room_id" to roomId)
        )
    }

    fun getWordFilter(roomId: Int): List<String> = db {
        query<String>(
            "SELECT `word` FROM `rooms_word_filter` WHERE `room_id` = :room_id",
            mapOf("room_id" to roomId)
        )
    }

    fun createRoom(userId: Int, name: String, description: String, model: String, category: Int, maxUsers: Int, tradeSettings: Int): Int {
        val roomId = roomSequence.nextId()
        WriteBehindManager.queue {
            db {
                update(
                    "INSERT INTO `rooms` (`id`, `name`, `description`, `owner_id`, `model_name`, `category`, `users_max`, `trade_state`) VALUES (:id, :name, :description, :owner_id, :model_name, :category, :users_max, :trade_state)",
                    mapOf(
                        "id" to roomId,
                        "name" to name,
                        "description" to description,
                        "owner_id" to userId,
                        "model_name" to model,
                        "category" to category,
                        "users_max" to maxUsers,
                        "trade_state" to tradeSettings
                    )
                )
            }
        }
        return roomId
    }

    fun saveItems(roomId: Int, roomItemsToSave: Collection<RoomItem>) {
        db {
            batchUpdate("UPDATE `items` SET `room_id` = :room_id, `x` = :x, `y` = :y, `z` = :z, `rot` = :rot, `wall_pos` = :wall_pos, `extra_data` = :extra_data WHERE `id` = :id",
                    roomItemsToSave.map {
                        val extraDataToSave = HabboServer.habboGame.itemManager
                            .getFurnitureLogic(it.furnishing)
                            .sanitizeForDatabase(it.extraData)
                        mapOf(
                                "room_id" to roomId,
                                "x" to it.position.x,
                                "y" to it.position.y,
                                "z" to it.position.z,
                                "rot" to it.rotation,
                                "wall_pos" to it.wallPosition,
                            "extra_data" to extraDataToSave,
                                "id" to it.id
                        )
                    }
            )
        }
    }

    fun updateRoomData(roomData: RoomData) {
        db {
            update(javaClass.classLoader.getResource("sql/rooms/data/update_room_data.sql")!!.readText(),
                    mapOf(
                        "name" to roomData.name,
                        "description" to roomData.description,
                        "state" to roomData.state.name.lowercase(Locale.getDefault()),
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
                            "group_id" to if (roomData.groupId == 0) null else roomData.groupId,
                            "model_name" to roomData.modelName,
                            "allow_navigator_dynamic_cats" to roomData.allowNavigatorDynamicCats,
                            "leave_on_door_tile_enabled" to roomData.leaveOnDoorTileEnabled,
                            "idle_sleep_enabled" to roomData.idleSleepEnabled,
                            "idle_sleep_timeout_seconds" to roomData.idleSleepTimeoutSeconds,
                            "idle_autokick_enabled" to roomData.idleAutokickEnabled,
                            "idle_autokick_timeout_seconds" to roomData.idleAutokickTimeoutSeconds,
                            "mute_all_pets" to roomData.muteAllPets,
                            "room_id" to roomData.id
                    )
            )
        }
    }

    fun addWordFilter(roomId: Int, wordFilter: String) {
        db {
            insertAndGetGeneratedKey("INSERT INTO `rooms_word_filter` (`room_id`, `word`) VALUES (:room_id, :word)",
                    mapOf(
                            "room_id" to roomId,
                            "word" to wordFilter
                    )
            )
        }
    }

    fun removeWordFilter(roomId: Int, wordFilter: String) {
        db {
            update("DELETE FROM `rooms_word_filter` WHERE `room_id` = :room_id AND `word` = :word",
                    mapOf(
                            "room_id" to roomId,
                            "word" to wordFilter
                    )
            )
        }
    }

    fun getFavoritesRooms(userId: Int): List<Pair<Int, Int>> = db {
        query<UserFavoriteRoomDto>(
            "SELECT `id`, `room_id` FROM `users_favorites` WHERE `user_id` = :user_id",
            mapOf("user_id" to userId)
        ).map { it.id to it.roomId }
    }

    fun addRight(userId: Int, roomId: Int): RightData {
        val id = db {
            insertAndGetGeneratedKey(javaClass.classLoader.getResource("sql/rooms/rights/insert_right.sql")!!.readText(),
                    mapOf(
                            "user_id" to userId,
                            "room_id" to roomId
                    )
            )
        }

        return RightData(id, userId)
    }

    fun removeRights(ids: List<Int>) {
        db {
            batchUpdate(javaClass.classLoader.getResource("sql/rooms/rights/delete_right.sql")!!.readText(),
                    ids.map {
                        mapOf(
                                "id" to it
                        )
                    }
            )
        }
    }

    fun updateCustomRoomModel(roomModel: RoomModel) {
        db {
            update(javaClass.classLoader.getResource("sql/rooms/model/update_custom_model.sql")!!.readText(),
                    mapOf(
                            "door_x" to roomModel.doorVector3.x,
                            "door_y" to roomModel.doorVector3.y,
                            "door_z" to roomModel.doorVector3.z,
                            "door_dir" to roomModel.doorDir,
                            "heightmap" to roomModel.heightmap.joinToString("\n"),
                            "id" to roomModel.id.toInt()
                    )
            )
        }
    }

    fun insertCustomRoomModel(roomModel: RoomModel) {
        val id = db {
            insertAndGetGeneratedKey(javaClass.classLoader.getResource("sql/rooms/model/insert_custom_model.sql")!!.readText(),
                    mapOf(
                            "room_id" to roomModel.roomId,
                            "door_x" to roomModel.doorVector3.x,
                            "door_y" to roomModel.doorVector3.y,
                            "door_z" to roomModel.doorVector3.z,
                            "door_dir" to roomModel.doorDir,
                            "heightmap" to roomModel.heightmap.joinToString("\n")
                    )
            )
        }

        roomModel.id = id.toString()
    }
}


data class RoomDataDto(
    val id: Int,
    val roomType: String = "open",
    val name: String,
    val ownerId: Int,
    val description: String = "",
    val category: Int = 0,
    val state: String = "open",
    val tradeState: Int = 0,
    val usersMax: Int = 25,
    val modelName: String,
    val score: Int = 0,
    val tags: String = "",
    val password: String = "",
    val wallpaper: String = "0.0",
    val floor: String = "0.0",
    val landscape: String = "0.0",
    val hideWall: Boolean = false,
    val wallThick: Int = 0,
    val wallHeight: Int = 0,
    val floorThick: Int = 0,
    val muteSettings: Int = 0,
    val banSettings: Int = 0,
    val kickSettings: Int = 0,
    val chatType: Int = 0,
    val chatBalloon: Int = 0,
    val chatSpeed: Int = 0,
    val chatMaxDistance: Int = 0,
    val chatFloodProtection: Int = 0,
    val allowPets: Boolean = true,
    val allowPetsEat: Boolean = false,
    val allowWalkThrough: Boolean = false,
    val groupId: Int? = 0,
    val allowNavigatorDynamicCats: Boolean = true,
    val leaveOnDoorTileEnabled: Boolean = true,
    val idleSleepEnabled: Boolean = true,
    val idleSleepTimeoutSeconds: Int = 1200,
    val idleAutokickEnabled: Boolean = false,
    val idleAutokickTimeoutSeconds: Int = 1800,
    val muteAllPets: Boolean = false
) {
    fun toDomain(): RoomData = RoomData(
        id,
        RoomType.valueOf(roomType.uppercase(Locale.getDefault())),
        name,
        ownerId,
        description,
        category,
        RoomState.valueOf(state.uppercase(Locale.getDefault())),
        tradeState,
        usersMax,
        modelName,
        score,
        tags.split(','),
        password,
        wallpaper,
        floor,
        landscape,
        hideWall,
        wallThick,
        wallHeight,
        floorThick,
        muteSettings,
        banSettings,
        kickSettings,
        chatType,
        chatBalloon,
        chatSpeed,
        chatMaxDistance,
        chatFloodProtection,
        allowPets,
        allowPetsEat,
        allowWalkThrough,
        groupId ?: 0,
        allowNavigatorDynamicCats,
        leaveOnDoorTileEnabled,
        idleSleepEnabled,
        idleSleepTimeoutSeconds,
        idleAutokickEnabled,
        idleAutokickTimeoutSeconds,
        muteAllPets
    )
}

data class RoomModelDto(
    val id: String,
    val roomId: Int = 0,
    val doorX: Int,
    val doorY: Int,
    val doorZ: Double,
    val doorDir: Int,
    val heightmap: String,
    val clubOnly: Boolean = false
) {
    fun toDomain() = RoomModel(
        id, roomId, Vector3(doorX, doorY, doorZ), doorDir,
        heightmap.trim().split("[\\r\\n]+".toRegex()), clubOnly
    )
}

data class UserFavoriteRoomDto(
    val id: Int,
    val roomId: Int
)


