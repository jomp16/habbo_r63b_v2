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

package ovh.rwx.habbo.game.item

import ovh.rwx.habbo.game.item.wired.WiredFurniSource
import ovh.rwx.habbo.game.item.wired.WiredUserSource

data class WiredData(
    val id: Int,
    var delay: Int, // delay
    var items: List<Int>, // items (stuffIds principais)
    var message: String, // text box
    var options: List<Int>, // options
    var extradata: String,  // extra data
    var filter: Boolean = false, // selector filter field
    var inverse: Boolean = false, // selector inverse field

    // --- NOVOS CAMPOS AIR / INPUT SOURCES ---
    var furniSources: List<WiredFurniSource> = emptyList(),
    var userSources: List<WiredUserSource> = emptyList(),
    var stuffIds2: List<Int> = emptyList()     // Lista secundária de itens selecionados
)