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

package ovh.rwx.habbo.game.snowwar.enums

import kotlin.random.Random

enum class SnowWarFieldType(val id: Int, val fieldName: String) {
    ARCTIC_ISLAND(8, "Ilha Ártica"),
    DRAGON_PEAK(9, "Topo do Dragão"),
    LOST_FOREST(10, "Floresta perdida"),
    FIGHT_NIGHT(11, "Noite de Luta"),
    BOBBA_LAKE(12, "Lago Bobba");

    companion object {
        fun fromId(id: Int): SnowWarFieldType {
            return entries.firstOrNull { it.id == id } ?: ARCTIC_ISLAND
        }

        fun fromQuery(query: String): SnowWarFieldType? {
            val q = query.trim().lowercase()
            val id = q.toIntOrNull()
            if (id != null) return entries.firstOrNull { it.id == id }
            return entries.firstOrNull {
                it.name.lowercase().contains(q) || it.fieldName.lowercase().contains(q)
            }
        }

        fun random(): SnowWarFieldType {
            val list = entries
            return list[Random.nextInt(list.size)]
        }
    }
}
