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

package ovh.rwx.habbo.game.item.xml

data class FurniXMLInfo(
        var spriteId: Int = 0,
        var itemName: String = "",
        var defaultDir: Int = 0,
        var xDim: Int = 0,
        var yDim: Int = 0,
        var canStandOn: Boolean = false,
        var canSitOn: Boolean = false,
        var canLayOn: Boolean = false,
        var wallFurni: Boolean = false,
        var customParams: String = "",
        var offerId: Int = -1
)