/*
 * Copyright (C) 2015-2019 jomp16 <root@rwx.ovh>
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
class RoomMannequinChangeFigureHandler {
    @Handler(Incoming.ROOM_MANNEQUIN_CHANGE_FIGURE)
    fun handle(habboSession: HabboSession, habboRequest: HabboRequest) {
        if (!habboSession.authenticated || habboSession.currentRoom == null || !habboSession.currentRoom!!.hasRights(habboSession)) return
        val itemId = habboRequest.readInt()
        val roomItem = habboSession.currentRoom!!.roomItems[itemId] ?: return

        if (roomItem.furnishing.interactionType != InteractionType.MANNEQUIN) return
        val split = roomItem.extraData.split(7.toChar()).toTypedArray()

        split[0] = habboSession.userInformation.gender.toLowerCase()
        split[1] = ""

        habboSession.userInformation.figure.split('.')
                .filter { !it.contains("hr") && !it.contains("hd") && !it.contains("he") && !it.contains("ea") && !it.contains("ha") }
                .forEach { split[1] = split[1] + it + '.' }

        split[1] = split[1].substring(0, split[1].lastIndexOf('.'))

        roomItem.extraData = split[0] + 7.toChar() + split[1] + 7.toChar() + split[2]

        roomItem.update(updateDb = true, updateClient = true)
    }
}