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

package ovh.rwx.habbo.communication.outgoing.room

import ovh.rwx.habbo.communication.*
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.OutgoingR63A

/**
 * Payload do GuestRoomResult/RoomEntryInfo (booleans de entrada de quarto) (GUEST_ROOM_RESULT).
 *
 * @param roomId ID do quarto.
 * @param isOwner Se o usuário é o dono do quarto.
 * @param isPrivate Se o quarto é privado (usado no R63A e early R63B).
 */
data class GuestRoomResultData(
    val roomId: Int,
    val isOwner: Boolean,
    val isPrivate: Boolean = true,
    val roomName: String = "",
)

@Suppress("unused", "UNUSED_PARAMETER")
class GuestRoomResultResponse {
    @Response(Outgoing.GUEST_ROOM_RESULT)
    @ResponseR63A(OutgoingR63A.GUEST_ROOM_RESULT)
    fun response(habboResponse: HabboResponse, data: GuestRoomResultData) {
        habboResponse.apply {
            // Evolução do RoomEntryInfoMessageEvent (header 2721@2013, 749@2016):
            //  - < 2009-06-30 (RELEASE34): int + bool + 2 bools mortos no AS3
            //  - 2009-06-30..2014-06-12: era dos public rooms — bool isGuestRoom abre branch:
            //      guest: guestRoomId + owner | public: publicSpaceDTO(name + id)
            //  - >= 2014-06-12: public rooms extintos, corpo achatado em int(roomId) + bool(owner)
            if (isVersionBetween(2009, 6, 30, 2014, 6, 12)) {
                writeBoolean(data.isPrivate) // isGuestRoom
                if (!data.isPrivate) {
                    // publicSpace: nome exibido no hotel view (_SafeStr_7797) antes do id
                    writeUTF(data.roomName)
                }
            }

            writeInt(data.roomId)
            writeBoolean(data.isOwner)

            if (isVersionBefore(2009, 6, 30)) {
                // RELEASE34 (2009-06-16): 2 campos não utilizados no AS3 (removidos na R35)
                writeBoolean(false) // Campo #3: _SafeStr_3500 (apenas impresso em Logger.log no AS3, sem efeito prático)
                writeBoolean(false) // Campo #4: _SafeStr_3502 (código morto, getter nunca chamado em nenhuma classe no AS3)
            }
        }
    }
}