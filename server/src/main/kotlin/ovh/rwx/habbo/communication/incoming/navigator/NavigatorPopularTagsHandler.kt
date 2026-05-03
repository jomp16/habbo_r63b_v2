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
import ovh.rwx.habbo.game.room.RoomType
import ovh.rwx.habbo.game.room.user.RoomUser
import ovh.rwx.habbo.game.user.HabboSession

@Suppress("unused", "UNUSED_PARAMETER")
class NavigatorPopularTagsHandler {
    @HandlerR63A(IncomingR63A.NAVIGATOR_POPULAR_TAGS)
    fun handleR63A(habboSession: HabboSession, habboRequest: HabboRequest) {
        val popularTags = mutableMapOf<String, Int>()

        HabboServer.habboGame.roomManager.rooms.values
            .filter { room ->
                !room.userManager.entities.values.filterIsInstance<RoomUser>()
                    .isEmpty() && room.roomData.roomType === RoomType.PRIVATE
            }
            .sortedBy { room -> room.userManager.entities.values.filterIsInstance<RoomUser>().size }
            .take(50)
            .forEach { room ->
                for (tag in room.roomData.tags) {
                    if (popularTags.containsKey(tag)) {
                        popularTags[tag] =
                            popularTags[tag]!! + room.userManager.entities.values.filterIsInstance<RoomUser>().size
                    } else {
                        popularTags[tag] = room.userManager.entities.values.filterIsInstance<RoomUser>().size
                    }
                }
            }

        habboSession.sendHabboResponse(OutgoingR63A.NAVIGATOR_POPULAR_TAGS, popularTags)
    }
}
