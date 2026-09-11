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
import ovh.rwx.habbo.game.room.Room
import ovh.rwx.habbo.game.user.HabboSession

data class RoomInfoData(
    val habboSession: HabboSession,
    val room: Room,
    val isLoading: Boolean,
    val checkEntry: Boolean
)

@Suppress("unused", "UNUSED_PARAMETER")
class RoomInfoResponse {
    @Response(Outgoing.ROOM_INFO)
    @ResponseR63A(OutgoingR63A.ROOM_INFO)
    fun response(habboResponse: HabboResponse, data: RoomInfoData) {
        habboResponse.apply {
            if (isVersionBefore(2011, 9, 20)) {
                writeBoolean(data.isLoading)

                serialize(data.room, true, data.isLoading)

                if (isVersionAtLeast(2009, 8, 21)) {
                    writeBoolean(data.checkEntry)
                }

                if (isVersionAtLeast(2010, 12, 3)) {
                    writeBoolean(false) // staff picked
                }
            } else {
                commonStuff(data.habboSession, data.isLoading, data.room, data.checkEntry)
                if (isVersionAtLeast(2025, 1, 23)) {
                    writeBoolean(data.isLoading) // openingConnection
                }
            }
        }
    }

    private fun HabboResponse.commonStuff(
        habboSession: HabboSession,
        isLoading: Boolean,
        room: Room,
        checkEntry: Boolean
    ) {
        writeBoolean(isLoading)

        serialize(room, true, isLoading)

        writeBoolean(checkEntry)
        writeBoolean(false)
        writeBoolean(false) // bypass bell, etc
        writeBoolean(false) // todo: room muted
        writeInt(room.roomData.muteSettings)
        writeInt(room.roomData.kickSettings)
        writeInt(room.roomData.banSettings)
        writeBoolean(room.userManager.hasRights(habboSession, true))
        writeInt(room.roomData.chatType)
        writeInt(room.roomData.chatBalloon)
        writeInt(room.roomData.chatSpeed)
        writeInt(room.roomData.chatMaxDistance)
        writeInt(room.roomData.chatFloodProtection)
    }
}