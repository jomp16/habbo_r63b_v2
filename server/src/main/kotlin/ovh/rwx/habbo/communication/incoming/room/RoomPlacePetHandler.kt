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
import ovh.rwx.habbo.communication.outgoing.room.RoomPetErrorNotificationResponse.RoomPetErrorNotification
import ovh.rwx.habbo.database.pet.PetDao
import ovh.rwx.habbo.game.user.HabboSession

@Suppress("unused", "UNUSED_PARAMETER")
class RoomPlacePetHandler {
    @Handler(Incoming.ROOM_PLACE_PET)
    @HandlerR63A(IncomingR63A.ROOM_PLACE_PET)
    fun handle(habboSession: HabboSession, habboRequest: HabboRequest) {
        val room = habboSession.currentRoom ?: return
        val petId = habboRequest.readInt()
        val x = habboRequest.readInt()
        val y = habboRequest.readInt()

        if (!room.roomData.allowPets && !room.userManager.hasRights(habboSession, ownerRight = true)) {
            habboSession.sendHabboResponse(
                Outgoing.ROOM_PET_ERROR_NOTIFICATION,
                RoomPetErrorNotification.PETS_FORBIDDEN_IN_ROOM
            )
            return
        }

        val petData = habboSession.habboInventory.removePet(petId) ?: return

        petData.roomId = room.roomData.id
        petData.x = x
        petData.y = y
        petData.z = room.roomGamemap.getAbsoluteHeight(x, y)
        PetDao.savePet(petData)

        room.userManager.addPet(petData)
    }
}
