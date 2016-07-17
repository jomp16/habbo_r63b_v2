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

package tk.jomp16.habbo.game.room

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import tk.jomp16.habbo.HabboServer
import tk.jomp16.habbo.communication.HabboResponse
import tk.jomp16.habbo.communication.IHabboResponseSerialize
import tk.jomp16.habbo.communication.outgoing.Outgoing
import tk.jomp16.habbo.database.item.ItemDao
import tk.jomp16.habbo.database.room.RoomDao
import tk.jomp16.habbo.game.item.ItemType
import tk.jomp16.habbo.game.item.room.RoomItem
import tk.jomp16.habbo.game.room.gamemap.RoomGamemap
import tk.jomp16.habbo.game.room.model.RoomModel
import tk.jomp16.habbo.game.room.tasks.AddUserToRoomTask
import tk.jomp16.habbo.game.room.tasks.RemoveUserFromRoomTask
import tk.jomp16.habbo.game.room.user.RoomUser
import tk.jomp16.habbo.game.user.HabboSession
import tk.jomp16.habbo.util.Utils
import tk.jomp16.habbo.util.Vector2
import tk.jomp16.habbo.util.Vector3
import tk.jomp16.utils.pathfinding.IFinder
import tk.jomp16.utils.pathfinding.core.finders.AStarFinder
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

class Room(val roomData: RoomData, val roomModel: RoomModel) : IHabboResponseSerialize {
    private val log: Logger = LoggerFactory.getLogger(javaClass)

    // for room task
    var roomTask: RoomTask? = null
    val emptyCounter: AtomicInteger = AtomicInteger()
    val saveRoomItemsCounter: AtomicInteger = AtomicInteger()
    val errorsCounter: AtomicInteger = AtomicInteger()

    val roomItems: MutableMap<Int, RoomItem> by lazy {
        HashMap(ItemDao.getRoomItems(roomData.id).associateBy { it.id })
    }
    val wallItems: Map<Int, RoomItem>
        get() = roomItems.filterValues { it.furnishing.type == ItemType.WALL }
    val floorItems: Map<Int, RoomItem>
        get() = roomItems.filterValues { it.furnishing.type == ItemType.FLOOR }

    val rights: MutableList<RightData> by lazy { ArrayList(RoomDao.getRights(roomData.id)) }

    val roomUsers: MutableMap<Int, RoomUser> by lazy { HashMap<Int, RoomUser>() }
    val roomUsersWithRights: List<RoomUser>
        get() = roomUsers.values.filter { hasRights(it.habboSession, false) }

    val roomGamemap: RoomGamemap by lazy { RoomGamemap(this) }
    val pathfinder: IFinder by lazy { AStarFinder() }

    private val roomItemsToSave: MutableList<RoomItem> by lazy { ArrayList<RoomItem>() }

    fun sendHabboResponse(headerId: Int, vararg args: Any) {
        // todo: find a way to cache habbo response

        roomUsers.values.forEach { it.habboSession?.sendHabboResponse(headerId, *args) }
    }

    fun hasRights(habboSession: HabboSession?, ownerRight: Boolean = false): Boolean {
        if (habboSession == null) return false

        val isOwner = roomData.ownerId == habboSession.userInformation.id /*&& habboSession.hasPermission("acc_any_room_owner")*/

        // todo: add groups
        return if (ownerRight) isOwner else isOwner || rights.any { it.userId == habboSession.userInformation.id }
    }

    fun addUser(habboSession: HabboSession) {
        roomTask?.let {
            // generate random virtual id
            var virtualId = 0

            while (virtualId == 0 || roomUsers.containsKey(virtualId)) virtualId = Utils.randInt(1..Int.MAX_VALUE)

            log.debug("Assigned virtual ID {} to user {}", virtualId, habboSession.userInformation.username)

            it.addTask(this, AddUserToRoomTask(
                    RoomUser(habboSession, this, virtualId, roomModel.doorVector3, roomModel.doorDir,
                             roomModel.doorDir)))
        }
    }

    fun removeUser(roomUser: RoomUser?, notifyClient: Boolean, kickNotification: Boolean) {
        if (roomUser == null) return

        if (roomUser.habboSession != null) {
            if (kickNotification) roomUser.habboSession.sendHabboResponse(Outgoing.GENERIC_ERROR, 4008)
            if (notifyClient) roomUser.habboSession.sendHabboResponse(Outgoing.ROOM_EXIT)

            roomUser.habboSession.roomUser = null
            roomUser.habboSession.currentRoom = null
            roomUser.habboSession.habboMessenger.notifyFriends()
        }

        roomGamemap.removeRoomUser(roomUser, roomUser.currentVector3.vector2)
        roomUsers.remove(roomUser.virtualID)

        roomTask?.let { it.addTask(this, RemoveUserFromRoomTask(roomUser)) }
    }

    override fun serializeHabboResponse(habboResponse: HabboResponse, vararg params: Any) {
        habboResponse.apply {
            val showEvents = params[0] as Boolean
            val enterRoom = params[1] as Boolean

            writeInt(roomData.id)
            writeUTF(roomData.caption)
            writeInt(roomData.ownerId)
            writeUTF(roomData.ownerName)
            writeInt(roomData.state.state)
            writeInt(roomUsers.size)
            writeInt(roomData.usersMax)
            writeUTF(roomData.description)
            writeInt(roomData.tradeState)
            writeInt(roomData.score)
            writeInt(0) // ranking
            writeInt(roomData.category)

            writeInt(roomData.tags.size)

            roomData.tags.forEach { writeUTF(it) }

            var value = if (enterRoom) 32 else 0

            // todo: groups
            /*val group = getGroup()

            if (group != null) {
                value += 2
            }*/

            if (showEvents) {
                // todo: events
                //value += 4;
            }

            if (roomData.roomType == RoomType.PRIVATE) value += 8

            if (roomData.allowPets) value += 16

            writeInt(value)

            // todo: groups
            /*if (group != null) {
                writeInt(group!!.getId())
                writeUTF(group!!.getName())
                writeUTF(group!!.getBadge())
            }*/
        }
    }

    fun addItemToSave(roomItem: RoomItem) {
        if (!roomItemsToSave.contains(roomItem)) roomItemsToSave += roomItem
    }

    fun saveItems() {
        if (roomItemsToSave.isEmpty()) return

        RoomDao.saveItems(roomData.id, roomItemsToSave)

        roomItemsToSave.clear()
    }

    fun setFloorItem(roomItem: RoomItem, position: Vector2, rotation: Int, userName: String): Boolean {
        val newItem = !roomItems.containsKey(roomItem.id)

        if (roomItem.position.vector2 == position && roomItem.rotation == rotation) return false

        HabboServer.habboGame.itemManager.getAffectedTiles(position.x, position.y, rotation, roomItem.furnishing.width,
                                                           roomItem.furnishing.height).forEach {
            if (!roomGamemap.grid.isWalkable(roomGamemap.grid, it.x, it.y)) {
                // cannot set item, because at least one tile is blocked

                return false
            }
        }

        if (!newItem) {
            roomGamemap.removeRoomItem(roomItem)

            // todo: remove user statuses too
        } else {
            roomItems += roomItem.id to roomItem
        }

        roomItem.position = Vector3(position.x, position.y,
                                    Utils.round(roomGamemap.getAbsoluteHeight(position.x, position.y), 2))

        roomGamemap.addRoomItem(roomItem)

        // todo: wired
        // todo: furni interactor
        // todo: roller

        if (newItem) {
            sendHabboResponse(Outgoing.ROOM_ITEM_ADDED, roomItem, userName)
        } else {
            sendHabboResponse(Outgoing.ROOM_FLOOR_ITEM_UPDATE, roomItem)
        }

        addItemToSave(roomItem)

        return true
    }
}