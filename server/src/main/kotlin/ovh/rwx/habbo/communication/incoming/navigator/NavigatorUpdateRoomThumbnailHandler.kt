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

package ovh.rwx.habbo.communication.incoming.navigator

import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.communication.HabboRequest
import ovh.rwx.habbo.communication.HandlerR63A
import ovh.rwx.habbo.communication.incoming.IncomingR63A
import ovh.rwx.habbo.communication.outgoing.OutgoingR63A
import ovh.rwx.habbo.game.user.HabboSession

@Suppress("unused", "UNUSED_PARAMETER")
class NavigatorUpdateRoomThumbnailHandler {
    @HandlerR63A(IncomingR63A.NAVIGATOR_UPDATE_ROOM_THUMBNAIL)
    fun handle(habboSession: HabboSession, habboRequest: HabboRequest) {
        val roomId = habboRequest.readInt()
        val room = HabboServer.habboGame.roomManager.rooms[roomId] ?: return
        if (!room.userManager.hasRights(habboSession, true)) return

        val backgroundImage = habboRequest.readInt()
        val foregroundImage = habboRequest.readInt()
        val itemCount = habboRequest.readInt()

        val items = mutableMapOf<Int, Int>()
        repeat(itemCount) {
            val slot = habboRequest.readInt()
            val itemId = habboRequest.readInt()
            if (slot in 0..10 && itemId in 1..27 && !items.containsKey(slot)) {
                items[slot] = itemId
            }
        }

        if (backgroundImage in 1..24 && foregroundImage in 0..11) {
            items.entries.joinToString("|") { "${it.key},${it.value}" }
//            room.roomData.iconBackground = backgroundImage
//            room.roomData.iconForeground = foregroundImage
//            room.roomData.iconItems = itemsStr
            // TODO: Save to database

            habboSession.sendHabboResponse(OutgoingR63A.NAVIGATOR_ROOM_THUMBNAIL_UPDATE_RESULT, roomId, 1)
        }
    }
}
