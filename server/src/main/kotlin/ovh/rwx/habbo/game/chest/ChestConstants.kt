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

package ovh.rwx.habbo.game.chest

object ChestConstants {
    const val CHUNK_SIZE = 200
    const val DEPOSIT_TIMEOUT_SECONDS = 60
    const val UPGRADE_COST = 10
    const val FURNI_CAPACITY_STEP = 1000
    const val COINS_CAPACITY_STEP = 5000
    const val FURNI_MAX_CAPACITY = 5000
    const val COINS_MAX_CAPACITY = 25000

    /**
     * Capacidades iniciais conforme a Central de Informações:
     * - Starter: 100
     * - Coins: 5000
     * - Furni: 1000
     */
    fun defaultCapacity(itemName: String): Int = when {
        itemName.contains("starter") -> 100
        itemName.contains("coins") -> 5000
        else -> 1000
    }

    /**
     * Extrai o valor de câmbio a partir do nome do item (ex: CF_10, CFC_50, CF_diamond_10).
     */
    fun getCreditFurniValue(itemName: String): Int? {
        if (!itemName.startsWith("CF_") && !itemName.startsWith("CFC_")) return null

        val split = itemName.split('_')

        return if (split.size > 2 && split[1] == "diamond") {
            split[2].toIntOrNull()
        } else if (split.size > 1) {
            split[1].toIntOrNull()
        } else {
            null
        }
    }
}
