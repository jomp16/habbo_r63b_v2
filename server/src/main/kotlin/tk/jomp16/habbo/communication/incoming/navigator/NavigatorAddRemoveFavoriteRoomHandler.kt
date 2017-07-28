/*
 * Copyright (C) 2015-2017 jomp16
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

package tk.jomp16.habbo.communication.incoming.navigator

import tk.jomp16.habbo.communication.HabboRequest
import tk.jomp16.habbo.communication.Handler
import tk.jomp16.habbo.communication.incoming.Incoming
import tk.jomp16.habbo.communication.outgoing.Outgoing
import tk.jomp16.habbo.database.navigator.NavigatorDao
import tk.jomp16.habbo.game.user.HabboSession

@Suppress("unused", "UNUSED_PARAMETER")
class NavigatorAddRemoveFavoriteRoomHandler {
    @Handler(Incoming.NAVIGATOR_ADD_FAVORITE_ROOM, Incoming.NAVIGATOR_REMOVE_FAVORITE_ROOM)
    fun handle(habboSession: HabboSession, habboRequest: HabboRequest) {
        if (!habboSession.authenticated) return
        val roomId = habboRequest.readInt()
        val addFavorite = habboRequest.incoming == Incoming.NAVIGATOR_ADD_FAVORITE_ROOM

        if (addFavorite && !habboSession.favoritesRooms.any { it.second == roomId }) {
            val id = NavigatorDao.addFavoriteRoom(habboSession.userInformation.id, roomId)
            habboSession.favoritesRooms.add(id to roomId)
        } else {
            val favoritePair = habboSession.favoritesRooms.filter { it.second == roomId }.firstOrNull() ?: return

            habboSession.favoritesRooms.remove(favoritePair)

            NavigatorDao.removeFavoriteRoom(favoritePair.first)
        }

        habboSession.sendHabboResponse(Outgoing.NAVIGATOR_UPDATE_FAVORITE_ROOM_STATUS, roomId, addFavorite)
    }
}