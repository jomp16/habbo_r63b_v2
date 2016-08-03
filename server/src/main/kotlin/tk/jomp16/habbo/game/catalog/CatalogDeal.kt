/*
 * Copyright (C) 2016 jomp16
 *
 * This file is part of habbo_r63b.
 *
 * habbo_r63b is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * habbo_r63b is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with habbo_r63b. If not, see <http://www.gnu.org/licenses/>.
 */

package tk.jomp16.habbo.game.catalog

import tk.jomp16.habbo.HabboServer
import tk.jomp16.habbo.game.item.Furnishing

data class CatalogDeal(
        val id: Int,
        val items: List<String>,
        val amounts: List<Int>
) {
    val furnishings: List<Furnishing>
        get() = HabboServer.habboGame.itemManager.furnishings.values.filter { items.contains(it.itemName) }
}