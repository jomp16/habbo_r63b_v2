/*
 * Copyright (C) 2015-2017 jomp16 <root@rwx.ovh>
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

package ovh.rwx.habbo.game.item.room

import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.communication.HabboResponse
import ovh.rwx.habbo.communication.IHabboResponseSerialize
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.database.item.ItemDao
import ovh.rwx.habbo.game.item.*
import ovh.rwx.habbo.game.item.wired.trigger.triggers.WiredTriggerWalksOffFurni
import ovh.rwx.habbo.game.item.wired.trigger.triggers.WiredTriggerWalksOnFurni
import ovh.rwx.habbo.game.room.Room
import ovh.rwx.habbo.game.room.user.RoomUser
import ovh.rwx.habbo.util.Rotation
import ovh.rwx.habbo.util.Vector2
import ovh.rwx.habbo.util.Vector3
import java.io.Serializable
import java.util.*

data class RoomItem(val id: Int, var userId: Int, var roomId: Int, val itemName: String, var extraData: String, var position: Vector3, var rotation: Int, var wallPosition: String) : IHabboResponseSerialize, Serializable {
    var magicRemove: Boolean = false
    private val limitedItemData: LimitedItemData? = ItemDao.getLimitedData(id)
    val wiredData: WiredData? by lazy { ItemDao.getWiredData(id) }
    val furnishing: Furnishing
        get() = HabboServer.habboGame.itemManager.furnishings[itemName]!!
    val room: Room
        get() = HabboServer.habboGame.roomManager.rooms[roomId]!!
    val height: Double
        get() {
            if (furnishing.stackMultiple) {
                if (extraData.isBlank()) {
                    extraData = "0"

                    update(true, true)
                }

                return furnishing.stackHeight[extraData.toInt()]
            }

            return furnishing.stackHeight[0]
        }
    val totalHeight: Double
        get() = position.z + height
    val affectedTiles: List<Vector2>
        get() = HabboServer.habboGame.itemManager.getAffectedTiles(position.x,
                position.y,
                rotation,
                furnishing.width,
                furnishing.height)
    private var cycles: Int = 0
    private var currentCycles: Int = 0
    val interactingUsers: MutableMap<Int, RoomUser> by lazy { HashMap<Int, RoomUser>() }

    override fun serializeHabboResponse(habboResponse: HabboResponse, vararg params: Any) {
        habboResponse.apply {
            if (furnishing.type == ItemType.FLOOR) {
                writeInt(id)
                writeInt(furnishing.spriteId)
                writeInt(position.x)
                writeInt(position.y)
                writeInt(rotation)
                writeUTF(position.z.toString())
                writeUTF(height.toString())

                HabboServer.habboGame.itemManager.writeExtradata(habboResponse, extraData, furnishing, limitedItemData, magicRemove)
            } else {
                writeUTF(id.toString())
                writeInt(furnishing.spriteId)
                writeUTF(wallPosition)
                writeUTF(if (furnishing.interactionType == InteractionType.POST_IT) extraData.split(' ')[0] else extraData)
            }

            writeInt(-1) // seems this is related to rentals (time in seconds)
            writeInt(if (furnishing.interactionModesCount > 1) 1 else 0)
            writeInt(userId) // todo: is builder ? -12345678 : userId
        }
    }

    fun update(updateDb: Boolean, updateClient: Boolean) {
        if (updateClient) {
            when {
                furnishing.type == ItemType.WALL -> room.sendHabboResponse(Outgoing.ROOM_WALL_ITEM_UPDATE, this)
                else -> room.sendHabboResponse(Outgoing.ROOM_FLOOR_ITEM_UPDATE, this)
            }
        }

        if (updateDb) room.addItemToSave(this)
    }

    fun addToRoom(room: Room, updateDb: Boolean, updateClient: Boolean, userName: String) {
        when {
            furnishing.type == ItemType.FLOOR -> {
                if (updateDb) room.addItemToSave(this)
                if (updateClient) room.sendHabboResponse(Outgoing.ROOM_ITEM_ADDED, this, userName)
            }
            furnishing.type == ItemType.WALL -> {
                if (updateDb) room.addItemToSave(this)
                if (updateClient) room.sendHabboResponse(Outgoing.ROOM_WALL_ITEM_ADDED, this, userName)
            }
        }
    }

    fun requestCycles(cycles1: Int) {
        if (currentCycles == 0 || cycles1 == 0) {
            cycles = cycles1
            currentCycles = 0
        }
    }

    fun onCycle() {
        if (cycles != 0 || furnishing.interactionType == InteractionType.ROLLER) {
            if (currentCycles++ >= cycles || furnishing.interactionType == InteractionType.ROLLER) {
                cycles = 0
                currentCycles = 0

                furnishing.interactor?.onCycle(room, this)

                return
            }
        }
    }

    fun onUserWalksOn(roomUser: RoomUser, handleInteractor: Boolean) {
        if (handleInteractor) furnishing.interactor?.onUserWalksOn(room, roomUser, this)

        room.wiredHandler.triggerWired(WiredTriggerWalksOnFurni::class, roomUser, this)
    }

    fun onUserWalksOff(roomUser: RoomUser, handleInteractor: Boolean) {
        if (handleInteractor) furnishing.interactor?.onUserWalksOff(room, roomUser, this)

        room.wiredHandler.triggerWired(WiredTriggerWalksOffFurni::class, roomUser, this)
    }

    fun canClose(): Boolean {
        var closeable = true

        affectedTiles.forEach {
            val roomUsers = room.roomGamemap.roomUserMap[it]

            if (roomUsers != null && closeable) closeable = roomUsers.isEmpty()
        }

        return closeable
    }

    private fun getFrontRotation(front: Vector2) = Rotation.calculate(front.x, front.y, position.x, position.y)

    fun getFrontRotation(): Int = when (rotation) {
        2 -> 6
        6 -> 2
        0 -> 4
        else -> 0
    }

    fun getFrontPosition(): Vector2 {
        var x = position.x
        var y = position.y

        when (rotation) {
            0 -> y--
            2 -> x++
            4 -> y++
            6 -> x--
        }

        return Vector2(x, y)
    }

    fun getBehindPosition(): Vector2 {
        var x = position.x
        var y = position.y

        when (rotation) {
            0 -> y++
            2 -> x--
            4 -> y--
            6 -> x++
        }

        return Vector2(x, y)
    }

    fun isTouching(pos: Vector3, rotation: Int, z: Double = (-1).toDouble()) = isTouching(pos, rotation, false, z)

    private fun isTouching(vector3: Vector3, rotation: Int, ignoreItemRotation: Boolean, z: Double): Boolean {
        if (z != (-1).toDouble() && z - vector3.z > 3.toDouble()) return false

        if (ignoreItemRotation) return !(rotation != -1 && rotation != getFrontRotation(vector3.vector2)) && (position.x == vector3.x && position.y == vector3.y ||
                vector3.x == position.x && vector3.y == position.y + 1 ||
                vector3.x == position.x - 1 && vector3.y == position.y + 1 ||
                vector3.x == position.x - 1 && vector3.y == position.y ||
                vector3.x == position.x + 1 && vector3.y == position.y + 1 ||
                vector3.x == position.x && vector3.y == position.y - 1 ||
                vector3.x == position.x + 1 && vector3.y == position.y - 1 ||
                vector3.x == position.x + 1 && vector3.y == position.y ||
                vector3.x == position.x - 1 && vector3.y == position.y - 1)

        return when {
            rotation != -1 && rotation != getFrontRotation() -> false
            position.x == vector3.x && position.y == vector3.y -> true
            else -> if (rotation == 2 || rotation == 6) vector3.x == (if (rotation == 6) position.x + 1 else position.x - 1) && vector3.y >= position.y && vector3.y < position.y + furnishing.width
            else vector3.y == (if (rotation == 0) position.y + 1 else position.y - 1) && vector3.x >= position.x && vector3.x < position.x + furnishing.height
        }
    }
}