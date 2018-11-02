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

package ovh.rwx.habbo.communication.outgoing.catalog

import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.communication.HabboResponse
import ovh.rwx.habbo.communication.Response
import ovh.rwx.habbo.communication.outgoing.Outgoing

@Suppress("unused", "UNUSED_PARAMETER")
class CatalogRecyclerRewardsResponse {
    @Response(Outgoing.CATALOG_RECYCLER_REWARDS)
    fun response(habboResponse: HabboResponse, recyclerRewards: Map<Int, List<String>>) {
        habboResponse.apply {
            writeInt(recyclerRewards.size) // levels
            recyclerRewards.entries.sortedByDescending { it.key }.forEach { entry ->
                writeInt(entry.key) // level
                writeInt(HabboServer.habboConfig.recyclerConfig.odds[entry.key]!!) // odds
                writeInt(entry.value.size)

                entry.value.forEach { s ->
                    HabboServer.habboGame.itemManager.furnishings[s]?.let { furnishing ->
                        writeUTF(furnishing.itemName)
                        writeInt(1) // enabled
                        writeUTF(furnishing.type.type)
                        writeInt(furnishing.spriteId)
                    }
                }
            }
        }
    }
}