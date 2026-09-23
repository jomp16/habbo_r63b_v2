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

package ovh.rwx.habbo.communication.incoming.wired

import ovh.rwx.habbo.communication.HabboRequest
import ovh.rwx.habbo.communication.Handler
import ovh.rwx.habbo.communication.incoming.Incoming
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.game.user.HabboSession

@Suppress("unused", "UNUSED_PARAMETER")
class WiredSetRoomSettingsHandler {
    @Handler(Incoming.WIRED_SET_ROOM_SETTINGS)
    fun handle(habboSession: HabboSession, habboRequest: HabboRequest) {
        val room = habboSession.currentRoom ?: return
        if (!room.userManager.hasRights(habboSession)) return

        val modifyPermissionMask = habboRequest.readInt()
        val readPermissionMask = habboRequest.readInt()
        val timezone = if (habboSession.habboVersion.isVersionAtLeast(2024, 12, 12)) {
            habboRequest.readUTF()
        } else {
            "UTC"
        }

        room.wiredRoomSettings.modifyPermissionMask = modifyPermissionMask
        room.wiredRoomSettings.readPermissionMask = readPermissionMask
        room.wiredRoomSettings.timezone = timezone

        habboSession.sendHabboResponse(Outgoing.WIRED_ROOM_SETTINGS, room.wiredRoomSettings)
    }
}
