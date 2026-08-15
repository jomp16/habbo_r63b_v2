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

package ovh.rwx.habbo.communication.incoming.user

import ovh.rwx.habbo.communication.HabboRequest
import ovh.rwx.habbo.communication.Handler
import ovh.rwx.habbo.communication.incoming.Incoming
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.user.BadgeLeaderboardResultResponse.BadgeLeaderboardEntry
import ovh.rwx.habbo.game.user.HabboSession

/**
 * GetBadgeLeaderboardMessageComposer.
 *
 * Campos enviados pelo cliente (AS3: _SafeStr_3256):
 *   new _SafeStr_3256(_arg_1.type, _arg_1.rarity, _arg_2, 50)
 *   1. type       - tipo do leaderboard
 *   2. rarity     - rarity id (0-6, tabela badge.rarity)
 *   3. chunkIndex - página/chunk do leaderboard
 *   4. 50         - tamanho do chunk (entries por request)
 */
@Suppress("unused", "UNUSED_PARAMETER")
class BadgeLeaderboardHandler {
    @Handler(Incoming.BADGE_LEADERBOARD)
    fun handle(habboSession: HabboSession, habboRequest: HabboRequest) {
        val type = habboRequest.readInt()
        val rarity = habboRequest.readInt()
        val chunkIndex = habboRequest.readInt()
        val chunkSize = habboRequest.readInt()

        habboSession.sendHabboResponse(
            Outgoing.BADGE_LEADERBOARD,
            type,
            rarity,
            chunkIndex,
            chunkSize,
            0, // TODO: totalEntries
            emptyList<BadgeLeaderboardEntry>() // TODO: entries reais do leaderboard
        )
    }
}
