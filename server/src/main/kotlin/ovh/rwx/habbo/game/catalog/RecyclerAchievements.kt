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

/**
 * Decisões puras das conquistas do Ecotron (sem dependência do servidor). `ACH_RecycleBoxOpener` NÃO está aqui: ela
 * conta na abertura da caixa (`GiftOpener`).
 */
object RecyclerAchievements {
    const val RECYCLED_ITEMS = "ACH_RecycledItems"
    const val RECYCLE_ECOPRIZES = "ACH_RecycleEcoprizes"
    const val RECYCLER_RARE_PRIZE = "ACH_RecyclerRarePrize"
    const val RECYCLER_GRAND_PRIZE = "ACH_RecyclerGrandPrize"
    const val RECYCLE_BOX_OPENER = "ACH_RecycleBoxOpener"

    /**
     * TODO(2025): em 2025 o Ecotron tem 6 faixas (comum/incomum/arcano/fenomenal/lendário/mítico); o modelo atual tem
     * níveis 1..5 (`recycler.odds`) e em 2012 eram comum(1)/incomum(2)/arcano(3). O mapeamento nível -> faixa precisa
     * do log oficial de pacotes (ver docs/HANDOFF_RECYCLER.md). Até lá, "arcano ou superior" = nível >= 3.
     */
    const val RARE_MIN_LEVEL = 3

    /**
     * Quanto progredir em cada conquista após uma reciclagem bem-sucedida (nome -> quantidade, sem zeros).
     *
     * @param recycledNames itemName de cada item reciclado
     * @param prizeNames itemName de todos os prêmios do reciclador (qualquer nível)
     * @param drawnLevel nível do prêmio sorteado
     * @param populatedLevels níveis que têm prêmios
     */
    fun progressFor(
        recycledNames: List<String>,
        prizeNames: Set<String>,
        drawnLevel: Int,
        populatedLevels: Set<Int>
    ): Map<String, Int> {
        val result = linkedMapOf<String, Int>()

        result[RECYCLED_ITEMS] = recycledNames.size
        // TODO(2025): as coleções giram e prêmios antigos deixam de ser recicláveis; o conjunto "é prêmio" pode
        //  precisar incluir temporadas passadas.
        result[RECYCLE_ECOPRIZES] = recycledNames.count { it in prizeNames }

        // TODO(2025): "arcano ou superior" depende das 6 faixas de 2025 (ver RARE_MIN_LEVEL e docs/HANDOFF_RECYCLER.md).
        if (drawnLevel >= RARE_MIN_LEVEL) result[RECYCLER_RARE_PRIZE] = 1

        // TODO(2025): grande prêmio = faixa mítica (0,02%); hoje = o maior nível que tem prêmios, desde que exista mais de
        //  um (só o comum preenchido não é "grande prêmio" em toda reciclagem).
        if (populatedLevels.size > 1 && drawnLevel == populatedLevels.max()) result[RECYCLER_GRAND_PRIZE] = 1

        return result.filterValues { it > 0 }
    }
}
