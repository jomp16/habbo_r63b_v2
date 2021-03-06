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

package ovh.rwx.habbo.communication.incoming.room

import ovh.rwx.habbo.communication.HabboRequest
import ovh.rwx.habbo.communication.Handler
import ovh.rwx.habbo.communication.incoming.Incoming
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.game.item.InteractionType
import ovh.rwx.habbo.game.user.HabboSession

@Suppress("unused", "UNUSED_PARAMETER")
class RoomApplyDecorationHandler {
    @Handler(Incoming.ROOM_APPLY_DECORATION)
    fun handle(habboSession: HabboSession, habboRequest: HabboRequest) {
        if (!habboSession.authenticated || habboSession.currentRoom == null) return
        val itemId = habboRequest.readInt()
        val userItem = habboSession.habboInventory.items[itemId] ?: return

        if (userItem.furnishing.interactionType != InteractionType.ROOM_EFFECT && (userItem.furnishing.itemName != "floor" || userItem.furnishing.itemName != "wallpaper" || userItem.furnishing.itemName != "landscape")) return
        val key = userItem.furnishing.itemName
        val value = userItem.extraData

        when (key) {
            "floor" -> habboSession.currentRoom?.roomData?.floor = value
            "wallpaper" -> habboSession.currentRoom?.roomData?.wallpaper = value
            "landscape" -> habboSession.currentRoom?.roomData?.landscape = value
        }

        habboSession.currentRoom?.sendHabboResponse(Outgoing.ROOM_DECORATION, key, value)

        habboSession.habboInventory.removeItems(listOf(itemId), true)
    }
}