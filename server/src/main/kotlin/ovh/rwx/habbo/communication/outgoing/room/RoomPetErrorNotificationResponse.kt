/*
 * Copyright (C) 2015-2018 jomp16 <root@rwx.ovh>
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

package ovh.rwx.habbo.communication.outgoing.room

import ovh.rwx.habbo.communication.HabboResponse
import ovh.rwx.habbo.communication.Response
import ovh.rwx.habbo.communication.outgoing.Outgoing

@Suppress("unused", "UNUSED_PARAMETER")
class RoomPetErrorNotificationResponse {
    @Response(Outgoing.ROOM_PET_ERROR_NOTIFICATION)
    fun response(habboResponse: HabboResponse, roomPetErrorNotification: RoomPetErrorNotification) {
        habboResponse.apply {
            writeInt(roomPetErrorNotification.errorCode)
        }
    }

    enum class RoomPetErrorNotification(val errorCode: Int) {
        PETS_FORBIDDEN_IN_HOTEL(0),
        PETS_FORBIDDEN_IN_ROOM(1),
        MAX_PETS(2),
        NO_FREE_TILES_FOR_PET(3),
        SELECTED_TILE_NOT_FREE_FOR_PET(4),
        MAX_NUMBER_OF_OWN_PETS(5)
    }
}