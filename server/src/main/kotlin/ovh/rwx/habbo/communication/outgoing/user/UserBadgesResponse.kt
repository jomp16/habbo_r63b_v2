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

import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.communication.HabboResponse
import ovh.rwx.habbo.communication.Response
import ovh.rwx.habbo.communication.ResponseR63A
import ovh.rwx.habbo.communication.isVersionAtLeast
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.OutgoingR63A
import ovh.rwx.habbo.game.user.badge.Badge

@Suppress("unused", "UNUSED_PARAMETER")
class UserBadgesResponse {
    @Response(Outgoing.USER_BADGES)
    @ResponseR63A(OutgoingR63A.USER_BADGES)
    fun response(habboResponse: HabboResponse, id: Int, badges: Collection<Badge>) {
        habboResponse.apply {
            val equippedBadges = badges.filter { it.slot > 0 }.sortedBy { it.slot }
            // todo: make sure if it's always user id or use virtualID when if in room
            if (habboResponse.habboVersion.majorVersion <= 38) {
                writeUTF(id.toString())
            } else {
                writeInt(id)
            }
            writeInt(equippedBadges.size)

            equippedBadges.forEach {
                writeInt(it.slot)
                writeUTF(it.code)
                if (habboResponse.isVersionAtLeast(2026, 8, 6)) {
                    writeInt(HabboServer.habboGame.badgeManager.getOwnerCount(it.code)) // ownerCount
                    // badgeRarityId -> rarity (classe AS3 com.sulake.habbo.communication.enum):
                    //   0 = COMMON    -> badge.rarity.common    (Comum)
                    //   1 = UNCOMMON  -> badge.rarity.uncommon  (Incomum - só com flag de config)
                    //   2 = RARE      -> badge.rarity.rare      (Raro)
                    //   3 = VERY_RARE -> badge.rarity.epic      (Épico)
                    //   4 = MYTHICAL  -> badge.rarity.mythical  (Místico)
                    //   5 = LEGENDARY -> badge.rarity.legendary (Lendário)
                    //   6 = UNIQUE    -> badge.rarity.unique    (Único)
                    writeInt(0) // todo: badgeRarityId
                }
            }
        }
    }
}