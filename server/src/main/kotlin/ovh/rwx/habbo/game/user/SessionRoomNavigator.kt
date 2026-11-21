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

package ovh.rwx.habbo.game.user

import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.plugin.event.events.room.RoomEnterEvent
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.OutgoingR63A
import ovh.rwx.habbo.communication.outgoing.misc.MiscGenericErrorResponse
import ovh.rwx.habbo.communication.outgoing.misc.MiscSuperNotificationResponse
import ovh.rwx.habbo.communication.outgoing.room.RoomDoorbellDeniedData
import ovh.rwx.habbo.game.room.Room
import ovh.rwx.habbo.game.room.RoomState
import ovh.rwx.habbo.game.room.user.RoomUser
import ovh.rwx.habbo.game.user.block.BlockPolicy

/**
 * Serializada por sessão: os pacotes de uma sessão rodam em paralelo (uma coroutine por pacote) e o cliente 2016
 * manda `ROOM_OPEN_FLAT` 3 vezes em ~10 ms depois de criar um quarto. Sem o lock, as entradas passavam juntas pelo
 * `room == currentRoom` (o currentRoom só é marcado depois de carregar a sala) e criavam duas entidades da mesma
 * sessão: a que a sessão não aponta ficava para sempre na sala (avatar fantasma).
 */
fun HabboSession.enterRoomImpl(room: Room, password: String = "", bypassAuth: Boolean = false) {
    synchronized(roomEnterLock) { enterRoomLocked(room, password, bypassAuth) }
}

private fun HabboSession.enterRoomLocked(room: Room, password: String, bypassAuth: Boolean) {
    if (!bypassAuth && room == currentRoom) return

    // pela sessão e não pelo roomUser: com o join anterior ainda pendente o roomUser é null, e removeEntity(null) não
    // fazia nada (a entidade ficava na sala antiga e o join dela ainda rodava depois)
    currentRoom?.let { previous ->
        previous.userManager.entities.values.filterIsInstance<RoomUser>()
            .filter { it.habboSession === this }
            .forEach { previous.userManager.removeEntity(it, notifyClient = false, kickNotification = false) }
    }

    // Carrega a sala (itens, wired, bans, variáveis, pets) em paralelo ANTES dos checks abaixo: eles tocam
    // itens e bans, que antes eram buscados em série na thread do handler. initialize() é idempotente/sincronizado.
    room.initialize()

    if (room.itemManager.hiddenBuildersClub && userInformation.id != room.roomData.ownerId) {
        if (!isPreShuffle) {
            sendHabboResponse(Outgoing.ROOM_EXIT)
            sendSuperNotification(MiscSuperNotificationResponse.MiscSuperNotificationKeys.BUILDERS_CLUB_VISIT_DENIED_GUEST)
        } else {
            sendHabboResponse(OutgoingR63A.ROOM_EXIT)
            sendNotification($$"${notification.builders_club.visit_denied_for_visitor.message}")
        }
        return
    }

    if (room.isBanned(userInformation.id) && !room.userManager.hasRights(this, true) && !hasPermission("acc_any_room_owner")) {
        if (!isPreShuffle) {
            sendHabboResponse(Outgoing.ROOM_ERROR, 4, "")
            sendHabboResponse(Outgoing.ROOM_EXIT)
        } else {
            sendHabboResponse(OutgoingR63A.ROOM_ERROR, 4)
            sendHabboResponse(OutgoingR63A.ROOM_EXIT)
        }
        return
    }

    // o dono bloqueou este usuário (o bloqueio vale mesmo para clientes antigos); staff ignora como no ban
    if (BlockPolicy.roomEntryDenied(habboBlocks, room.roomData.ownerId, hasPermission("acc_any_room_owner"))) {
        if (!isPreShuffle) {
            sendHabboResponse(Outgoing.ROOM_ERROR, 5, "")
            sendHabboResponse(Outgoing.ROOM_EXIT)
        } else {
            sendHabboResponse(OutgoingR63A.ROOM_ERROR, 5)
            sendHabboResponse(OutgoingR63A.ROOM_EXIT)
        }
        return
    }

    if (!room.running) HabboServer.habboGame.roomManager.roomTaskManager.addRoom(room)

    if (room.userManager.entities.values.filterIsInstance<RoomUser>().size >= room.roomData.usersMax && !room.userManager.hasRights(
            this,
            true
        ) && !hasPermission("acc_enter_full_room")
    ) {
        if (!isPreShuffle) {
            sendHabboResponse(Outgoing.ROOM_ERROR, 1, "")
            sendHabboResponse(Outgoing.ROOM_EXIT)
        } else {
            sendHabboResponse(OutgoingR63A.ROOM_ERROR, 1)
            sendHabboResponse(OutgoingR63A.ROOM_EXIT)
        }
        return
    }

    val loading = !bypassAuth && !room.userManager.hasRights(this, true)

    if (loading) {
        if (room.roomData.state == RoomState.PASSWORD && !HabboServer.habboGame.passwordEncryptor.checkPassword(
                password,
                room.roomData.password
            )
        ) {
            if (!isPreShuffle) {
                sendHabboResponse(
                    Outgoing.MISC_GENERIC_ERROR,
                    MiscGenericErrorResponse.MiscGenericError.WRONG_PASSWORD
                )
                sendHabboResponse(Outgoing.ROOM_EXIT)
            } else {
                sendHabboResponse(
                    OutgoingR63A.MISC_GENERIC_ERROR,
                    MiscGenericErrorResponse.MiscGenericError.WRONG_PASSWORD
                )
                sendHabboResponse(OutgoingR63A.ROOM_EXIT)
            }
            return
        } else if (room.roomData.state == RoomState.LOCKED) {
            val roomUsersWithRights = room.userManager.usersWithRights

            if (roomUsersWithRights.isEmpty()) {
                sendHabboResponse(
                    Outgoing.ROOM_DOORBELL_DENIED,
                    RoomDoorbellDeniedData(username = "", roomId = room.roomData.id)
                )

                sendResponse(Outgoing.ROOM_EXIT, OutgoingR63A.ROOM_EXIT)
            } else {
                currentRoom = room

                roomUsersWithRights.forEach {
                    (it as? RoomUser)?.habboSession?.let { habboSession ->
                        habboSession.sendResponse(Outgoing.ROOM_DOORBELL, OutgoingR63A.ROOM_DOORBELL, userInformation.username)
                    }
                }

                sendResponse(Outgoing.ROOM_DOORBELL, OutgoingR63A.ROOM_DOORBELL, "")
            }
            return
        }
    }

    userStats.roomVisits++
    currentRoom = room

    userStats.favoriteGroup?.let {
        room.loadedGroups.add(it)
        room.sendResponse(Outgoing.ROOM_GROUP_BADGES, OutgoingR63A.ROOM_GROUPS_BADGES, room.loadedGroups)
    }

    room.userManager.addUser(this)

    HabboServer.pluginManager.executeEventAsync(RoomEnterEvent(room, this))
}
