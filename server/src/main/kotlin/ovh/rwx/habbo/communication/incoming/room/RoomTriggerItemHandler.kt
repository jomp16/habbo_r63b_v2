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
import ovh.rwx.habbo.game.item.InteractionType
import ovh.rwx.habbo.game.user.HabboSession

@Suppress("unused", "UNUSED_PARAMETER")
class RoomTriggerItemHandler {
    @Handler(Incoming.ROOM_TRIGGER_ITEM, Incoming.ROOM_TRIGGER_WALL_ITEM, Incoming.ROOM_TRIGGER_ONE_WAY_GATE, Incoming.ROOM_TRIGGER_HABBO_WHEEL, Incoming.ROOM_TRIGGER_CLOSE_DICE, Incoming.ROOM_TRIGGER_ROLL_DICE)
    fun handle(habboSession: HabboSession, habboRequest: HabboRequest) {
        if (!habboSession.authenticated || habboSession.currentRoom == null) return

        habboSession.currentRoom?.let {
            val item = it.roomItems[habboRequest.readInt()] ?: return@let

            if (item.furnishing.itemName == "external_image_wallitem_poster_small") return@let
            val interactor = item.furnishing.interactor

            when {
                interactor != null -> interactor.onTrigger(it, habboSession.roomUser, item, it.hasRights(habboSession), habboRequest.readInt())
                else -> {
                    if (item.furnishing.interactionType != InteractionType.NOT_FOUND) habboSession.sendNotification("No interactor for this furnishing!\n\nInteractor type:\n\n${item.furnishing.interactionType.name}")
                    else habboSession.sendNotification("I don't know which interactor this furni uses!\n\nItem name: ${item.itemName}")
                }
            }
        }
    }
}