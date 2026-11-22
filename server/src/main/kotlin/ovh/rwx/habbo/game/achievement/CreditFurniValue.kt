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

package ovh.rwx.habbo.game.achievement

/**
 * Valor em créditos de um mobi de Habbo Câmbio (`CF_<valor>`, `CFC_<valor>`, `CF_diamond_<valor>`); 0 para qualquer
 * outro item. Base das conquistas de moedas (ACH_PlaceCreditValue, ACH_CoinValueTraded).
 */
object CreditFurniValue {
    fun of(itemName: String): Int {
        if (!itemName.startsWith("CF_") && !itemName.startsWith("CFC_")) return 0

        val split = itemName.split('_')
        return when {
            split.size > 2 && split[1] == "diamond" -> split[2].toIntOrNull() ?: 0
            split.size > 1 -> split[1].toIntOrNull() ?: 0
            else -> 0
        }
    }
}
