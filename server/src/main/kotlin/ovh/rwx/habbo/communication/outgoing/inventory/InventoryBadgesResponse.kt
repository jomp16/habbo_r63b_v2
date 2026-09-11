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

package ovh.rwx.habbo.communication.outgoing.inventory

import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.communication.HabboResponse
import ovh.rwx.habbo.communication.Response
import ovh.rwx.habbo.communication.ResponseR63A
import ovh.rwx.habbo.communication.isVersionAtLeast
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.OutgoingR63A
import ovh.rwx.habbo.game.user.badge.Badge

data class InventoryBadgesData(val badges: Collection<Badge>)

@Suppress("unused", "UNUSED_PARAMETER")
class InventoryBadgesResponse {
    @Response(Outgoing.INVENTORY_BADGES)
    @ResponseR63A(OutgoingR63A.INVENTORY_BADGES)
    fun response(habboResponse: HabboResponse, data: InventoryBadgesData) {
        habboResponse.apply {
            if (isVersionAtLeast(2019, 1, 14)) {
                writeInt(1) // totalFragments
                writeInt(0) // fragmentNo

                writeInt(data.badges.size)

                data.badges.forEach {
                    writeInt(it.id)
                    writeUTF(it.code)
                    if (isVersionAtLeast(2026, 6, 1)) {
                        writeInt(HabboServer.habboGame.badgeManager.getOwnerCount(it.code)) // ownerCount
                        writeInt(0) // badgeRarityId
                    }
                }
            } else {
                val equippedBadges = data.badges.filter { it.slot > 0 }

                writeInt(data.badges.size)

                data.badges.forEach {
                    if (isVersionAtLeast(2011, 5, 23)) {
                        writeInt(1)
                    }
                    writeUTF(it.code)
                }

                writeInt(equippedBadges.size)

                equippedBadges.forEach {
                    writeInt(it.slot)
                    writeUTF(it.code)
                }
            }
        }
    }
}
