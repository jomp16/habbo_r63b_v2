/*
 * Copyright (C) 2016 jomp16
 *
 * This file is part of habbo_r63b.
 *
 * habbo_r63b is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * habbo_r63b is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with habbo_r63b. If not, see <http://www.gnu.org/licenses/>.
 */

package tk.jomp16.habbo.game.item.room

import tk.jomp16.habbo.HabboServer
import tk.jomp16.habbo.communication.HabboResponse
import tk.jomp16.habbo.communication.IHabboResponseSerialize
import tk.jomp16.habbo.communication.outgoing.Outgoing
import tk.jomp16.habbo.game.item.Furnishing
import tk.jomp16.habbo.game.item.InteractionType
import tk.jomp16.habbo.game.item.ItemType
import tk.jomp16.habbo.game.item.LimitedItemData
import tk.jomp16.habbo.game.room.Room
import tk.jomp16.habbo.util.Vector2
import tk.jomp16.habbo.util.Vector3

data class RoomItem(
        val id: Int,
        var userId: Int,
        var roomId: Int,
        val baseName: String,
        var extraData: String,
        val limitedId: Int,
        var position: Vector3,
        var rotation: Int,
        var wallPosition: String,
        val limitedItemData: LimitedItemData?
) : IHabboResponseSerialize {
    val furnishing: Furnishing
        get() = HabboServer.habboGame.itemManager.furnishings[baseName]!!

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
        get() = HabboServer.habboGame.itemManager.getAffectedTiles(position.x, position.y, rotation, furnishing.width, furnishing.height)

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

                HabboServer.habboGame.itemManager.writeExtradata(habboResponse, extraData, furnishing, limitedItemData)
            } else {
                writeUTF(id.toString())
                writeInt(furnishing.spriteId)
                writeUTF(wallPosition)
                writeUTF(if (furnishing.interactionType == InteractionType.POST_IT) extraData.split(' ')[0] else extraData)
            }

            writeInt(-1)
            writeInt(if (furnishing.interactionModesCount > 1) 1 else 0)
            writeInt(userId) // todo: is builder ? -12345678 : userId
        }
    }

    fun update(updateDb: Boolean, updateClient: Boolean) {
        if (updateClient) {
            when {
                furnishing.type == ItemType.WALL -> room.sendHabboResponse(Outgoing.ROOM_WALL_ITEM_UPDATE, this)
                else                             -> room.sendHabboResponse(Outgoing.ROOM_FLOOR_ITEM_UPDATE, this)
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
            furnishing.type == ItemType.WALL  -> {
                if (updateDb) room.addItemToSave(this)
                if (updateClient) room.sendHabboResponse(Outgoing.ROOM_WALL_ITEM_ADDED, this, userName)
            }
        }
    }
}