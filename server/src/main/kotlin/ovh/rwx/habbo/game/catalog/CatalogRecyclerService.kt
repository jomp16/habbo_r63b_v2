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

package ovh.rwx.habbo.game.catalog

import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.game.item.Furnishing
import kotlin.random.Random

class CatalogRecyclerService {

    fun getRandomRecyclerLevel(recyclerRewards: Map<Int, List<String>>): Int {
        val odds = HabboServer.habboConfig.recyclerConfig.odds
        val availableLevels = odds.entries
            .filter { it.key != 1 && it.key in recyclerRewards }
            .sortedByDescending { it.key }

        for ((level, chance) in availableLevels) {
            if ((1..chance).random() == chance) return level
        }

        return 1
    }

    fun getRandomRecyclerReward(recyclerRewards: Map<Int, List<String>>): RecyclerDraw? {
        val furnishings = HabboServer.habboGame.itemManager.furnishings
        // só sorteia entre prêmios que existem como mobi (nome inválido no banco não faz o reciclador falhar)
        val valid = recyclerRewards.mapValues { (_, names) -> names.filter { furnishings.containsKey(it) } }
        val level = RecyclerRewards.drawLevel(valid, HabboServer.habboConfig.recyclerConfig.odds, Random.Default)
            ?: return null
        val furnishing = furnishings[valid.getValue(level).random(Random.Default)] ?: return null

        return RecyclerDraw(level, furnishing)
    }
}

/** Prêmio sorteado do Ecotron e o nível em que saiu (as conquistas de prêmio raro dependem do nível). */
data class RecyclerDraw(val level: Int, val furnishing: Furnishing)
