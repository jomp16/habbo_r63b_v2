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
import ovh.rwx.habbo.database.pet.PetDao
import ovh.rwx.habbo.game.room.user.RoomPet
import ovh.rwx.habbo.game.user.HabboSession

@Suppress("unused", "UNUSED_PARAMETER")
class RoomTakePetHandler {
    @Handler(Incoming.ROOM_TAKE_PET)
    @HandlerR63A(IncomingR63A.ROOM_TAKE_PET)
    fun handle(habboSession: HabboSession, habboRequest: HabboRequest) {
        val room = habboSession.currentRoom ?: return
        val petId = habboRequest.readInt()

        val roomPet = room.userManager.entities.values
            .filterIsInstance<RoomPet>()
            .find { it.petData.id == petId } ?: return

        // Only owner or room owner can take the pet
        if (roomPet.petData.userId != habboSession.userInformation.id &&
            !room.userManager.hasRights(habboSession, ownerRight = true)
        ) return

        roomPet.syncPosition()
        roomPet.petData.roomId = 0
        PetDao.savePet(roomPet.petData)

        room.userManager.removeEntity(roomPet, notifyClient = false, kickNotification = false)

        // Add back to owner's inventory
        val ownerSession = habboSession.takeIf { it.userInformation.id == roomPet.petData.userId }
        ownerSession?.habboInventory?.addPet(roomPet.petData)
    }
}
