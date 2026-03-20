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

package ovh.rwx.habbo.game.room

import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.communication.HabboResponse
import ovh.rwx.habbo.communication.IHabboResponseSerialize
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.OutgoingR63A
import ovh.rwx.habbo.database.group.GroupDao
import ovh.rwx.habbo.database.room.RoomDao
import ovh.rwx.habbo.game.group.Group
import ovh.rwx.habbo.game.room.gamemap.RoomGamemap
import ovh.rwx.habbo.game.room.games.RoomGameManager
import ovh.rwx.habbo.game.room.games.RoomGameType
import ovh.rwx.habbo.game.room.managers.RoomItemManager
import ovh.rwx.habbo.game.room.managers.RoomNetworkDispatcher
import ovh.rwx.habbo.game.room.managers.RoomUserManager
import ovh.rwx.habbo.game.room.model.RoomModel
import ovh.rwx.habbo.pathfinding.IFinder
import ovh.rwx.habbo.pathfinding.core.DiagonalMovement
import ovh.rwx.habbo.pathfinding.core.finders.AStarFinder
import ovh.rwx.habbo.pathfinding.core.heuristics.EuclideanHeuristic
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.atomic.AtomicInteger

class Room(val roomData: RoomData, var roomModel: RoomModel) : IHabboResponseSerialize {
    // region Managers
    val networkDispatcher = RoomNetworkDispatcher(this)
    val userManager = RoomUserManager(this)
    val itemManager = RoomItemManager(this)
    // endregion

    // region Counters & State
    var roomTask: RoomTask? = null
    val rollerCounter = AtomicInteger()
    val emptyCounter = AtomicInteger()
    val errorsCounter = AtomicInteger()
    val roomTimer = AtomicInteger()
    val hostingCounter = AtomicInteger()
    private var initialized: Boolean = false

    val rolledItemsThisTick = CopyOnWriteArraySet<Int>()
    val rolledUsersThisTick = CopyOnWriteArraySet<Int>()
    // endregion

    // region Sub-Managers & Helpers
    lateinit var roomGamemap: RoomGamemap
    val pathfinder: IFinder by lazy { AStarFinder(DiagonalMovement.ALWAYS, EuclideanHeuristic()) }
    val gameManager: RoomGameManager by lazy { RoomGameManager(this) }
    // endregion

    val wordFilter: MutableSet<String> by lazy { HashSet(RoomDao.getWordFilter(roomData.id)) }

    val group: Group? get() = if (roomData.groupId == 0) null else HabboServer.habboGame.groupManager.groups[roomData.groupId]
    val loadedGroups: MutableSet<Group> by lazy { HashSet() }
    // endregion

    // region Initialization
    fun initialize() {
        if (initialized) return

        itemManager.loadItems()
        roomGamemap = RoomGamemap(this)
        itemManager.triggerItems()

        if (itemManager.items.values.any { it.furnishing.interactionType.name.startsWith("BATTLE_BANZAI") }) {
            gameManager.registerGame(RoomGameType.BATTLE_BANZAI)
        }

        group?.let { loadedGroups.add(it) }
        initialized = true
    }
    // endregion

    // region Network / Broadcast
    fun sendHabboResponse(habboResponse: HabboResponse) {
        networkDispatcher.sendResponse(habboResponse)
    }

    fun sendHabboResponse(outgoing: Outgoing, vararg args: Any?) {
        networkDispatcher.sendResponseModern(outgoing, *args)
    }

    fun sendHabboResponse(outgoing: OutgoingR63A, vararg args: Any?) {
        networkDispatcher.sendResponseR63A(outgoing, *args)
    }
    // endregion

    fun updateGroupInfo() {
        userManager.updateGroupInfo()
    }

    fun updateGroupRights() {
        userManager.updateGroupRights()
    }
    // endregion

    fun saveRoom() {
        RoomDao.updateRoomData(roomData)
        group?.let { GroupDao.updateGroupData(it.groupData) }
        itemManager.savePendingItems()
    }
    // endregion

    // region Serialization
    override fun serializeHabboResponse(habboResponse: HabboResponse, vararg params: Any) {
        habboResponse.apply {
            val enterRoom = params.getOrNull(1) as? Boolean ?: false

            writeInt(roomData.id)
            writeUTF(roomData.name)
            writeInt(roomData.ownerId)
            writeUTF(roomData.ownerName)
            writeInt(roomData.state.state)
            writeInt(userManager.users.size)
            writeInt(roomData.usersMax)
            writeUTF(roomData.description)
            writeInt(roomData.tradeState)
            writeInt(roomData.score)
            writeInt(0) // ranking
            writeInt(roomData.category)

            writeInt(roomData.tags.size)
            roomData.tags.forEach { writeUTF(it) }

            var bitMaskValue = if (enterRoom) 32 else 0
            if (group != null) bitMaskValue += 2
            if (roomData.roomType == RoomType.PRIVATE) bitMaskValue += 8
            if (roomData.allowPets) bitMaskValue += 16

            writeInt(bitMaskValue)

            group?.let {
                writeInt(it.groupData.id)
                writeUTF(it.groupData.name)
                writeUTF(it.groupData.badge)
            }
        }
    }

    override fun serializeHabboResponseR63A(habboResponse: HabboResponse, vararg params: Any) {
        habboResponse.apply {
            writeInt(roomData.id)
            writeBoolean(false) // is event
            writeUTF(roomData.name)
            writeUTF(roomData.ownerName)
            writeInt(roomData.state.state)
            writeInt(userManager.users.size)
            writeInt(roomData.usersMax)
            writeUTF(roomData.description)
            writeInt(roomData.tradeState) // srchSpecPrm
            writeBoolean(roomData.tradeState == 1) // allowTrading
            writeInt(roomData.score)
            writeInt(roomData.category)
            writeUTF("") // eventCreationTime

            writeInt(roomData.tags.size)
            roomData.tags.forEach { writeUTF(it) }

            // room icon defaults
            writeInt(1)
            writeInt(0)
            writeInt(0)

            writeBoolean(roomData.allowPets)
            writeBoolean(roomData.allowPetsEat)
        }
    }
    // endregion
}
