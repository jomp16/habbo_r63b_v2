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

package ovh.rwx.habbo.communication.outgoing.catalog.recycler

import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.communication.HabboResponse
import ovh.rwx.habbo.communication.Response
import ovh.rwx.habbo.communication.ResponseR63A
import ovh.rwx.habbo.communication.isVersionAtLeast
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.OutgoingR63A
import ovh.rwx.habbo.game.item.Furnishing

@Suppress("unused", "UNUSED_PARAMETER")
class CatalogRecyclerRewardsResponse {
    @Response(Outgoing.CATALOG_RECYCLER_REWARDS)
    fun response(habboResponse: HabboResponse, recyclerRewards: Map<Int, List<String>>) {
        val levels = resolveLevels(recyclerRewards)

        habboResponse.apply {
            writeInt(levels.size) // levels
            levels.forEach { level ->
                writeInt(level.level) // level
                writeInt(level.odds) // odds
                writeInt(level.prizes.size)

                level.prizes.forEach { furnishing ->
                    // até 2012-04 (build 201204021456) o prêmio é só `tipo + spriteId`; o nome e a quantidade
                    // (bundle) surgem em 2012-05-02 (habbo-timeline; AS3 20111213/20120704 conferem)
                    if (isVersionAtLeast(2012, 5, 2)) {
                        writeUTF(furnishing.itemName)
                        writeInt(1) // todo: it isn't enabled, and yes item count (could be a bundle)
                    }
                    writeUTF(furnishing.type.type) // productItemType
                    writeInt(furnishing.spriteId) // productItemTypeId
                }
            }
        }
    }

    @ResponseR63A(OutgoingR63A.CATALOG_RECYCLER_REWARDS)
    fun responseR63A(habboResponse: HabboResponse, recyclerRewards: Map<Int, List<String>>) {
        val levels = resolveLevels(recyclerRewards)

        habboResponse.apply {
            writeInt(levels.size) // levels
            levels.forEach { level ->
                writeInt(level.level) // level
                writeInt(level.odds) // odds
                writeInt(level.prizes.size)

                level.prizes.forEach { furnishing ->
                    writeUTF(furnishing.type.type) // productItemType
                    writeInt(furnishing.spriteId) // productItemTypeId
                }
            }
        }
    }

    private class RecyclerLevel(val level: Int, val odds: Int, val prizes: List<Furnishing>)

    /**
     * Resolve tudo ANTES de escrever: a contagem de cada nível é a dos prêmios que têm mobi (um nome do banco sem
     * furnishing seria contado e não escrito, desalinhando o buffer do client) e a falta de odds falha antes do
     * primeiro byte. Do nível mais raro para o mais comum.
     */
    private fun resolveLevels(recyclerRewards: Map<Int, List<String>>): List<RecyclerLevel> {
        val furnishings = HabboServer.habboGame.itemManager.furnishings
        val odds = HabboServer.habboConfig.recyclerConfig.odds

        return recyclerRewards.entries.sortedByDescending { it.key }.map { (level, names) ->
            RecyclerLevel(
                level,
                odds[level] ?: error("Can't find the odds for recycler level $level!"),
                names.mapNotNull { furnishings[it] }
            )
        }
    }
}