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

package ovh.rwx.habbo.communication.incoming.room

import ovh.rwx.habbo.communication.HabboRequest
import ovh.rwx.habbo.communication.Handler
import ovh.rwx.habbo.communication.HandlerR63A
import ovh.rwx.habbo.communication.incoming.Incoming
import ovh.rwx.habbo.communication.incoming.IncomingR63A
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.OutgoingR63A
import ovh.rwx.habbo.game.room.Room
import ovh.rwx.habbo.game.room.user.RoomPet
import ovh.rwx.habbo.game.user.HabboSession

@Suppress("unused", "UNUSED_PARAMETER")
class PetRespectHandler {
    @Handler(Incoming.PET_RESPECT)
    @HandlerR63A(IncomingR63A.PET_RESPECT)
    fun handle(habboSession: HabboSession, habboRequest: HabboRequest) {
        val room = habboSession.currentRoom ?: return
        if (habboSession.userStats.dailyPetRespectPoints <= 0) return

        val petId = habboRequest.readInt()

        val roomPet = room.userManager.entities.values
            .filterIsInstance<RoomPet>()
            .find { it.petData.id == petId } ?: return

        habboSession.userStats.dailyPetRespectPoints--
        roomPet.petData.respect++
        roomPet.petData.happiness = (roomPet.petData.happiness + 10).coerceAtMost(100)

        // +10 XP per respect
        addExperience(roomPet, room, 10)

        room.sendHabboResponse(Outgoing.PET_RESPECT_NOTIFICATION, roomPet.petData)
        room.sendHabboResponse(OutgoingR63A.PET_RESPECT_NOTIFICATION, roomPet.petData)
    }

    private fun addExperience(roomPet: RoomPet, room: Room, amount: Int) {
        val oldLevel = roomPet.petData.level
        roomPet.petData.experience += amount

        if (roomPet.petData.level > oldLevel) {
            // todo: send level up notification
        }

        room.sendHabboResponse(Outgoing.PET_EXPERIENCE, roomPet.petData.id, roomPet.virtualID, amount)
        room.sendHabboResponse(OutgoingR63A.PET_EXPERIENCE, roomPet.petData.id, roomPet.virtualID, amount)
    }
}
