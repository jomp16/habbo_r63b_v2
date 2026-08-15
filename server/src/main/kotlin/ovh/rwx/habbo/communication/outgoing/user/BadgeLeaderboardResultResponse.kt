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

package ovh.rwx.habbo.communication.outgoing.user

import ovh.rwx.habbo.communication.HabboResponse
import ovh.rwx.habbo.communication.Response
import ovh.rwx.habbo.communication.outgoing.Outgoing

/**
 * BadgeLeaderboardResultMessageEvent.
 *
 * Estrutura do parser AS3 (_SafeStr_3635):
 *   writeInt(type)
 *   writeInt(rarity)        // 0-6 (tabela badge.rarity)
 *   writeInt(page)          // página/chunk
 *   writeInt(size)          // tamanho do chunk (50)
 *   writeInt(totalEntries)
 *   writeInt(entries.size)
 *   entries.forEach {       // BadgeLeaderboardEntry (_SafeStr_2998)
 *       writeInt(userId)
 *       writeUTF(userName)
 *       writeUTF(figureString)
 *       writeInt(rank)
 *       writeInt(score)
 *   }
 *   writeBoolean(ownEntry != null)
 *   if (ownEntry != null) { // mesma estrutura da entry acima
 *       ...
 *   }
 */
@Suppress("unused", "UNUSED_PARAMETER")
class BadgeLeaderboardResultResponse {
    @Response(Outgoing.BADGE_LEADERBOARD)
    fun response(
        habboResponse: HabboResponse,
        type: Int,
        rarity: Int,
        page: Int,
        size: Int,
        totalEntries: Int,
        entries: Collection<BadgeLeaderboardEntry>
    ) {
        habboResponse.apply {
            writeInt(type)
            writeInt(rarity)
            writeInt(page)
            writeInt(size)
            writeInt(totalEntries)
            writeInt(entries.size)

            entries.forEach {
                writeInt(it.userId)
                writeUTF(it.userName)
                writeUTF(it.figure)
                writeInt(it.rank)
                writeInt(it.score)
            }

            writeBoolean(false) // TODO: ownEntry
        }
    }

    data class BadgeLeaderboardEntry(
        val userId: Int,
        val userName: String,
        val figure: String,
        val rank: Int,
        val score: Int
    )
}
