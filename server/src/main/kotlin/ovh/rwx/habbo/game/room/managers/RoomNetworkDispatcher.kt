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

import ovh.rwx.habbo.communication.HabboResponse
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.OutgoingR63A
import ovh.rwx.habbo.game.room.Room
import ovh.rwx.habbo.game.room.user.RoomUser

class RoomNetworkDispatcher(private val room: Room) {
    fun sendResponse(habboResponse: HabboResponse) {
        room.userManager.entities.values.filterIsInstance<RoomUser>()
            .forEach { it.habboSession.sendHabboResponse(habboResponse) }
    }

    fun sendResponseModern(outgoing: Outgoing, vararg args: Any?) {
        room.userManager.entities.values.filterIsInstance<RoomUser>().map { it.habboSession }
            .filter { it.release != "R63A" }
            .forEach { it.sendHabboResponse(outgoing, *args) }
    }

    fun sendResponseR63A(outgoing: OutgoingR63A, vararg args: Any?) {
        room.userManager.entities.values.filterIsInstance<RoomUser>().map { it.habboSession }
            .filter { it.release == "R63A" }
            .forEach { it.sendHabboResponse(outgoing, *args) }
    }
}