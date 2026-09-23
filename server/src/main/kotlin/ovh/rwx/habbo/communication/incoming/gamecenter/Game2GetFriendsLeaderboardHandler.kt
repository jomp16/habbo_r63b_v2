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

package ovh.rwx.habbo.communication.incoming.gamecenter

import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.communication.HabboRequest
import ovh.rwx.habbo.communication.Handler
import ovh.rwx.habbo.communication.incoming.Incoming
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.game.snowwar.SnowWarTotalLeaderboardData
import ovh.rwx.habbo.game.user.HabboSession

@Suppress("unused", "UNUSED_PARAMETER")
class Game2GetFriendsLeaderboardHandler {
    @Handler(Incoming.GAME_2_GET_FRIENDS_LEADERBOARD)
    fun handle(habboSession: HabboSession, habboRequest: HabboRequest) {
        val gameTypeId = habboRequest.readInt()
        val rank = habboRequest.readInt()
        val direction = habboRequest.readInt()
        val pageSize = habboRequest.readInt()
        val maxEntries = habboRequest.readInt()

        val (entries, totalListSize) = HabboServer.habboGame.snowWarManager.getFriendsLeaderboard(
            habboSession,
            gameTypeId,
            rank,
            direction,
            pageSize,
            maxEntries
        )

        habboSession.sendHabboResponse(
            Outgoing.GAME_2_FRIENDS_LEADERBOARD,
            SnowWarTotalLeaderboardData(
                entries = entries,
                totalListSize = totalListSize,
                gameTypeId = gameTypeId
            )
        )
    }
}
