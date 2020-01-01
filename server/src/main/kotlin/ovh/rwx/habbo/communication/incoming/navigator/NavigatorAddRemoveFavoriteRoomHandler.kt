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

package ovh.rwx.habbo.communication.incoming.navigator

import ovh.rwx.habbo.communication.HabboRequest
import ovh.rwx.habbo.communication.Handler
import ovh.rwx.habbo.communication.incoming.Incoming
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.database.navigator.NavigatorDao
import ovh.rwx.habbo.game.user.HabboSession

@Suppress("unused", "UNUSED_PARAMETER")
class NavigatorAddRemoveFavoriteRoomHandler {
    @Handler(Incoming.NAVIGATOR_ADD_FAVORITE_ROOM, Incoming.NAVIGATOR_REMOVE_FAVORITE_ROOM)
    fun handle(habboSession: HabboSession, habboRequest: HabboRequest) {
        val roomId = habboRequest.readInt()
        val addFavorite = habboRequest.incoming == Incoming.NAVIGATOR_ADD_FAVORITE_ROOM

        if (addFavorite && !habboSession.favoritesRooms.any { it.second == roomId }) {
            val id = NavigatorDao.addFavoriteRoom(habboSession.userInformation.id, roomId)
            habboSession.favoritesRooms.add(id to roomId)
        } else {
            val favoritePair = habboSession.favoritesRooms.firstOrNull { it.second == roomId } ?: return

            habboSession.favoritesRooms.remove(favoritePair)

            NavigatorDao.removeFavoriteRoom(favoritePair.first)
        }

        habboSession.sendHabboResponse(Outgoing.NAVIGATOR_UPDATE_FAVORITE_ROOM_STATUS, roomId, addFavorite)
    }
}