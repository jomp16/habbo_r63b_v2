/*
 * Copyright (C) 2016 jomp16
 *
 * This file is part of habbo_r63b.
 *
 * habbo_r63b is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * habbo_r63b is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with habbo_r63b. If not, see <http://www.gnu.org/licenses/>.
 */

package tk.jomp16.habbo.communication.incoming.navigator

import tk.jomp16.habbo.HabboServer
import tk.jomp16.habbo.communication.HabboRequest
import tk.jomp16.habbo.communication.Handler
import tk.jomp16.habbo.communication.incoming.Incoming
import tk.jomp16.habbo.communication.outgoing.Outgoing
import tk.jomp16.habbo.game.user.HabboSession

@Suppress("unused", "UNUSED_PARAMETER")
class NavigatorCreateRoomHandler {
    @Handler(Incoming.NAVIGATOR_CREATE_ROOM)
    fun handle(habboSession: HabboSession, habboRequest: HabboRequest) {
        if (!habboSession.authenticated) return

        // [0][5]asdfg[0][6]asdfgh[0][7]model_a[0][0][0][1][0][0][0]2[0][0][0][2]

        // todo: perform sanity check here, pretty please?
        val name = habboRequest.readUTF()
        val description = habboRequest.readUTF()
        val model = habboRequest.readUTF()
        val category = habboRequest.readInt()
        val maxUsers = habboRequest.readInt() // 10 = min, 50 = max
        val tradeSettings = habboRequest.readInt() // 2 = All can trade, 1 = owner only, 0 = no trading.

        val room = HabboServer.habboGame.roomManager.createRoom(habboSession.userInformation.id,
                habboSession.habboSubscription.validUserSubscription,
                name, description, model, category, maxUsers,
                tradeSettings) ?: return

        habboSession.rooms.add(room)

        // FlatCreatedComposer id, name

        habboSession.sendHabboResponse(Outgoing.NAVIGATOR_CREATE_ROOM, room.roomData.id, room.roomData.caption)
    }
}