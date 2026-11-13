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

import kotlin.random.Random

/** Sorteio puro do prêmio do Ecotron (sem dependência do servidor, testável com [Random] semeado). */
object RecyclerRewards {
    /**
     * Sorteia o nível: do mais raro ao mais comum, cada nível com prêmios (exceto o 1) acerta com chance `1/odds`;
     * se nenhum acerta, cai no nível 1. Níveis sem prêmios nunca são sorteados.
     */
    fun drawLevel(rewards: Map<Int, List<String>>, odds: Map<Int, Int>, random: Random): Int? {
        val populated = rewards.filterValues { it.isNotEmpty() }
        if (populated.isEmpty()) return null

        for ((level, chance) in odds.entries.filter { it.key != 1 && it.key in populated && it.value > 0 }
            .sortedByDescending { it.key }) {
            if (random.nextInt(chance) == 0) return level
        }

        // nível 1 sem prêmios: usa o nível povoado mais comum (menor), para nunca falhar havendo algum prêmio
        return if (1 in populated) 1 else populated.keys.min()
    }

    /** Sorteia o nome do prêmio (uniforme dentro do nível); null só se não existe nenhum prêmio. */
    fun draw(rewards: Map<Int, List<String>>, odds: Map<Int, Int>, random: Random): String? {
        val level = drawLevel(rewards, odds, random) ?: return null

        return rewards.getValue(level).random(random)
    }
}
