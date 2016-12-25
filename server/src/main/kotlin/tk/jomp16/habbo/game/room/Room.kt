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

package tk.jomp16.habbo.game.room

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import tk.jomp16.habbo.HabboServer
import tk.jomp16.habbo.communication.HabboResponse
import tk.jomp16.habbo.communication.IHabboResponseSerialize
import tk.jomp16.habbo.communication.outgoing.Outgoing
import tk.jomp16.habbo.database.item.ItemDao
import tk.jomp16.habbo.database.room.RoomDao
import tk.jomp16.habbo.game.item.InteractionType
import tk.jomp16.habbo.game.item.ItemType
import tk.jomp16.habbo.game.item.room.RoomItem
import tk.jomp16.habbo.game.room.dimmer.RoomDimmer
import tk.jomp16.habbo.game.room.gamemap.RoomGamemap
import tk.jomp16.habbo.game.room.model.RoomModel
import tk.jomp16.habbo.game.room.tasks.UserJoinRoomTask
import tk.jomp16.habbo.game.room.tasks.UserPartRoomTask
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
    val errorsCounter: AtomicInteger = AtomicInteger()

    val roomItems: MutableMap<Int, RoomItem> by lazy { HashMap(ItemDao.getRoomItems(roomData.id).associateBy { it.id }) }
    val wallItems: Map<Int, RoomItem>
        get() = roomItems.filterValues { it.furnishing.type == ItemType.WALL }
    val floorItems: Map<Int, RoomItem>
        get() = roomItems.filterValues { it.furnishing.type == ItemType.FLOOR }

    val rights: MutableList<RightData> by lazy { ArrayList(RoomDao.getRights(roomData.id)) }
    val wordFilter: MutableSet<String> by lazy { HashSet(RoomDao.getWordFilter(roomData.id)) }

    val roomUsers: MutableMap<Int, RoomUser> by lazy { HashMap<Int, RoomUser>() }
    val roomUsersWithRights: List<RoomUser>
        get() = roomUsers.values.filter { hasRights(it.habboSession, false) }

    val roomGamemap: RoomGamemap by lazy { RoomGamemap(this) }
    val pathfinder: IFinder by lazy { AStarFinder() }

    private val roomItemsToSave: MutableList<RoomItem> by lazy { ArrayList<RoomItem>() }

    var roomDimmer: RoomDimmer? = null

    fun initialize() {
        roomItems.values.filter { it.furnishing.interactor != null }.forEach { it.furnishing.interactor?.onPlace(this, null, it) }
        roomItems.values.filter { it.furnishing.interactionType == InteractionType.DIMMER }.firstOrNull()?.let { roomDimmer = ItemDao.getRoomDimmer(it) }
    }

    fun sendHabboResponse(headerId: Int, vararg args: Any) {
        // todo: find a way to cache habbo response

        roomUsers.values.forEach { it.habboSession?.sendHabboResponse(headerId, *args) }
    }

    fun hasRights(habboSession: HabboSession?, ownerRight: Boolean = false): Boolean {
        if (habboSession == null) return false

        val isOwner = roomData.ownerId == habboSession.userInformation.id && habboSession.hasPermission("acc_any_room_owner")

        // todo: add groups
        return if (ownerRight) isOwner else isOwner || rights.any { it.userId == habboSession.userInformation.id }
    }

    fun addUser(habboSession: HabboSession) {
        roomTask?.let {
            if (roomUsers.values.filter { it.habboSession == habboSession }.isNotEmpty()) return

            // generate random virtual id
            var virtualId: Int

            do {
                virtualId = Utils.randInt(1..Int.MAX_VALUE)
            } while (roomUsers.containsKey(virtualId))

            log.debug("Assigned virtual ID {} to user {}", virtualId, habboSession.userInformation.username)

            it.addTask(this, UserJoinRoomTask(RoomUser(habboSession, this, virtualId, roomModel.doorVector3, roomModel.doorDir, roomModel.doorDir)))
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

        roomTask?.addTask(this, UserPartRoomTask(roomUser))
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

    fun saveQueuedItems() {
        if (roomItemsToSave.isEmpty()) return

        RoomDao.saveItems(roomData.id, roomItemsToSave)

        if (roomDimmer != null && roomItemsToSave.any { it == roomDimmer!!.roomItem }) ItemDao.saveDimmer(roomDimmer!!)

        roomItemsToSave.clear()
    }

    fun setFloorItem(roomItem: RoomItem, position: Vector2, rotation: Int, roomUser: RoomUser?, overrideZ: Double = -1.toDouble()): Boolean {
        val newItem = !roomItems.containsKey(roomItem.id)

        if (roomItem.position.vector2 == position && roomItem.rotation == rotation) return false

        HabboServer.habboGame.itemManager.getAffectedTiles(position.x, position.y, rotation, roomItem.furnishing.width, roomItem.furnishing.height).forEach {
            if (roomItem.rotation == rotation && roomGamemap.cannotStackItem[it.x][it.y] && roomGamemap.isBlocked(it, true) && overrideZ == -1.toDouble()) {
                // cannot set item, because at least one tile is blocked

                return false
            }
        }

        val affectedTiles = mutableSetOf<Vector2>()

        if (!newItem) {
            roomGamemap.removeRoomItem(roomItem)

            HabboServer.habboGame.itemManager.getAffectedTiles(roomItem.position.x, roomItem.position.y, roomItem.rotation, roomItem.furnishing.width, roomItem.furnishing.height).let {
                it.forEach { vector2 ->
                    roomGamemap.getUsersFromVector2(vector2).forEach(RoomUser::removeUserStatuses)
                }

                affectedTiles += it
            }
        }

        roomItem.position = Vector3(position.x, position.y, if (overrideZ != -1.toDouble()) overrideZ else roomGamemap.getAbsoluteHeight(position.x, position.y))
        roomItem.rotation = rotation

        roomGamemap.addRoomItem(roomItem)

        roomItem.affectedTiles.let {
            it.forEach { vector2 ->
                roomGamemap.getUsersFromVector2(vector2).forEach {
                    it.addUserStatuses(roomItem)
                }
            }

            affectedTiles += it
        }

        // todo: wired
        roomItem.furnishing.interactor?.onPlace(this, roomUser, roomItem)
        // todo: roller

        if (newItem) {
            roomItems.put(roomItem.id, roomItem)

            roomItem.addToRoom(this, true, true, roomUser?.habboSession?.userInformation?.username ?: "")
        } else {
            roomItem.update(true, true)
        }

        sendHabboResponse(Outgoing.ROOM_UPDATE_FURNI_STACK, this, affectedTiles)

        return true
    }

    fun setWallItem(roomItem: RoomItem, wallData: List<String>, roomUser: RoomUser?): Boolean {
        if (wallData.size != 3 || !wallData[0].startsWith(":w=") || !wallData[1].startsWith("l=") || wallData[2] != "r" && wallData[2] != "l") return false

        val newItem = !roomItems.containsKey(roomItem.id)

        val wBit = wallData[0].substring(3, wallData[0].length)
        val lBit = wallData[1].substring(2, wallData[1].length)

        if (!wBit.contains(",") || !lBit.contains(",")) return false

        val wBitSplit = wBit.split(",".toRegex())
        val lBitSplit = lBit.split(",".toRegex())

        val w1 = wBitSplit[0].toInt()
        val w2 = wBitSplit[1].toInt()
        val l1 = lBitSplit[0].toInt()
        val l2 = lBitSplit[1].toInt()

        if (w1 < 0 || w2 < 0 || l1 < 0 || l2 < 0 || w1 > 200 || w2 > 200 || l1 > 200 || l2 > 200) return false

        roomItem.wallPosition = ":w=$w1,$w2 l=$l1,$l2 ${wallData[2]}"

        roomItem.furnishing.interactor?.onPlace(this, roomUser, roomItem)

        if (newItem) {
            if (roomItem.furnishing.interactionType == InteractionType.DIMMER) {
                // todo: dimmer
                if (roomDimmer != null) return false

                roomDimmer = ItemDao.getRoomDimmer(roomItem)
                roomItem.extraData = roomDimmer!!.generateExtraData()
            }

            roomItems.put(roomItem.id, roomItem)

            roomItem.addToRoom(this, true, true, roomUser?.habboSession?.userInformation?.username ?: "")
        } else {
            roomItem.update(true, true)
        }

        return true
    }

    fun removeItem(roomUser: RoomUser, roomItem: RoomItem): Boolean {
        if (!roomItems.containsValue(roomItem)) return false

        roomItems.remove(roomItem.id)
        roomGamemap.removeRoomItem(roomItem)

        // todo: WIRED
        if (roomItem.furnishing.interactionType == InteractionType.DIMMER) {
            ItemDao.saveDimmer(roomDimmer!!)

            roomDimmer = null
        }

        roomItem.furnishing.interactor?.onRemove(this, roomUser, roomItem)

        @Suppress("NON_EXHAUSTIVE_WHEN")
        when (roomItem.furnishing.type) {
            ItemType.FLOOR -> {
                sendHabboResponse(Outgoing.ROOM_FLOOR_ITEM_REMOVE, roomItem, false, 0)

                HabboServer.habboGame.itemManager.getAffectedTiles(roomItem.position.x, roomItem.position.y, roomItem.rotation, roomItem.furnishing.width, roomItem.furnishing.height).let {
                    it.forEach { vector2 ->
                        roomGamemap.getUsersFromVector2(vector2).forEach(RoomUser::removeUserStatuses)
                    }

                    sendHabboResponse(Outgoing.ROOM_UPDATE_FURNI_STACK, this, it)
                }
            }
            ItemType.WALL -> {
                sendHabboResponse(Outgoing.ROOM_WALL_ITEM_REMOVE, roomItem)
            }
        }

        if (roomItemsToSave.contains(roomItem)) roomItemsToSave.remove(roomItem)

        return true
    }
}