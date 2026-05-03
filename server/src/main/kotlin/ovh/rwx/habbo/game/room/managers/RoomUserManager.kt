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

package ovh.rwx.habbo.game.room.managers

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.OutgoingR63A
import ovh.rwx.habbo.communication.outgoing.misc.MiscGenericErrorResponse
import ovh.rwx.habbo.database.room.RoomDao
import ovh.rwx.habbo.game.room.RightData
import ovh.rwx.habbo.game.room.Room
import ovh.rwx.habbo.game.room.tasks.UserJoinRoomTask
import ovh.rwx.habbo.game.room.tasks.UserPartRoomTask
import ovh.rwx.habbo.game.room.user.RoomUser
import ovh.rwx.habbo.game.user.HabboSession

class RoomUserManager(private val room: Room) {
    private val log: Logger = LoggerFactory.getLogger(javaClass)

    val rights: MutableSet<RightData> by lazy { HashSet(RoomDao.getRights(room.roomData.id)) }
    val users: MutableMap<Int, RoomUser> by lazy { HashMap() }
    val usersWithRights: Set<RoomUser> get() = users.values.filter { hasRights(it.habboSession, false) }.toSet()

    fun hasRights(
        habboSession: HabboSession?,
        ownerRight: Boolean = false,
        ignorePermissionAnyRoomOwner: Boolean = false
    ): Boolean {
        if (habboSession == null) return false

        val userId = habboSession.userInformation.id
        val isOwner =
            room.roomData.ownerId == userId || (!ignorePermissionAnyRoomOwner && habboSession.hasPermission("acc_any_room_owner"))

        if (ownerRight) return isOwner
        if (isOwner) return true

        room.group?.let { g ->
            val isGroupAdmin = g.admins.any { it.userId == userId } || habboSession.hasPermission("acc_any_group_admin")
            if (g.groupData.onlyAdminCanDecorateRoom) {
                if (isGroupAdmin) return true
            } else {
                if (g.members.any { it.userId == userId } || isGroupAdmin) return true
            }
        }

        return rights.any { it.userId == userId }
    }

    fun addUser(habboSession: HabboSession) {
        if (room.roomTask == null || users.values.any { it.habboSession == habboSession }) return

        var virtualId: Int

        do {
            virtualId = (1..Int.MAX_VALUE).random()
        } while (users.containsKey(virtualId))

        val newUser =
            RoomUser(
                habboSession,
                room,
                virtualId,
                room.roomModel.doorVector3,
                room.roomModel.doorDir,
                room.roomModel.doorDir
            )

        // Adiciona o usuário imediatamente ao mapa para permitir remoção em caso de disconnect
        // O estado pendingJoin=true indica que o join ainda não foi completado
        users[virtualId] = newUser

        // Enfileira a tarefa de join que vai completar a inicialização e definir pendingJoin=false
        room.roomTask?.addTask(room, UserJoinRoomTask(newUser))
    }

    fun removeUser(roomUser: RoomUser?, notifyClient: Boolean, kickNotification: Boolean) {
        if (roomUser == null) return

        // Cancela qualquer troca ativa do usuário antes de remover
        room.tradeManager.onUserDisconnect(roomUser)

        roomUser.habboSession?.let { session ->
            handleUserDisconnectionMessages(session, notifyClient, kickNotification)
            if (session.currentRoom == room) {
                session.roomUser = null
                session.currentRoom = null
                session.habboMessenger.notifyFriends()
            }
        }

        // Remove do gamemap (se o usuário já foi adicionado)
        // Usuários em pendingJoin podem ainda não estar no gamemap
        if (!roomUser.pendingJoin || roomUser.currentVector3.vector2 != room.roomModel.doorVector3) {
            room.roomGamemap.removeRoomUser(roomUser, roomUser.currentVector3.vector2)
        }
        roomUser.nextStepVector?.let { room.roomGamemap.removeRoomUser(roomUser, it.vector2) }

        users.remove(roomUser.virtualID)

        // Só envia UserPartRoomTask se o usuário já tinha completado o join
        // Usuários em pendingJoin nunca foram oficialmente adicionados ao room tick
        if (!roomUser.pendingJoin) {
            room.gameManager.onUserLeaveRoom(roomUser)
            room.roomTask?.addTask(room, UserPartRoomTask(roomUser))
        }
    }

    private fun handleUserDisconnectionMessages(
        session: HabboSession,
        notifyClient: Boolean,
        kickNotification: Boolean
    ) {
        val isR63A = session.release == "R63A"

        if (kickNotification) {
            val outMsg = if (isR63A) OutgoingR63A.MISC_GENERIC_ERROR else Outgoing.MISC_GENERIC_ERROR
            session.sendAnyResponse(outMsg, MiscGenericErrorResponse.MiscGenericError.ROOM_KICKED)
        }

        if (notifyClient) {
            val exitMsg = if (isR63A) OutgoingR63A.ROOM_EXIT else Outgoing.ROOM_EXIT
            session.sendAnyResponse(exitMsg)
        }
    }

    fun updateGroupInfo() {
        room.group?.let { g ->
            users.values.mapNotNull { it.habboSession }.forEach { session ->
                session.sendHabboResponse(
                    Outgoing.GROUP_INFO,
                    session.userInformation.id,
                    session.userStats.favoriteGroupId == g.groupData.id,
                    g,
                    false
                )
            }
        }
    }

    fun updateGroupRights() {
        val currentGroup = room.group ?: return

        users.values.mapNotNull { it.habboSession }
            .filter { it.userInformation.id != currentGroup.groupData.ownerId }
            .forEach { session ->
                val roomUser = users.values.find { it.habboSession == session } ?: return@forEach
                val methodName =
                    HabboServer.habboHandler.getOverrideMethodForHeader(Outgoing.ROOM_OWNER, session.release)

                val hasPermission = hasRights(session, false)
                val statusKey = "flatctrl"

                if (hasPermission) {
                    roomUser.addStatus(statusKey, "1")
                    dispatchRightsResponse(session, methodName, 1)
                } else if (roomUser.statusMap.containsKey(statusKey)) {
                    roomUser.removeStatus(statusKey)
                    dispatchRightsResponse(session, methodName, 0)
                }
            }
    }

    private fun dispatchRightsResponse(session: HabboSession, methodName: String, level: Int) {
        when (methodName) {
            "response" -> session.sendHabboResponse(Outgoing.ROOM_RIGHT_LEVEL, level)
            "responseWithRoomId" -> session.sendHabboResponse(Outgoing.ROOM_RIGHT_LEVEL, room.roomData.id, level)
            else -> log.error("Couldn't send response for right level!")
        }
    }
}