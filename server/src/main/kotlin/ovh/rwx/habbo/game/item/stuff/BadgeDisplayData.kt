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

package ovh.rwx.habbo.game.item.stuff

import ovh.rwx.habbo.game.item.logic.FurnitureLogic

/**
 * Badge display (interaction_type = badge_display).
 *
 * Correlação AS3: StringArrayStuffData (FORMAT_KEY 2) com STATE_DEFAULT_INDEX = 0,
 * usado pelo furniture_badge_display. Índices do array do client:
 *  [0] state ("0"), [1] badge name, [2] owner username, [3] display date.
 *
 * Formato no banco (extra_data): badgeName[SEP]ownerName[SEP]displayedAt
 */
data class BadgeDisplayData(
    val badgeName: String,
    val ownerName: String,
    val displayedAt: String,
    val state: String = "0",
) {
    fun toStuffData(): StringArrayStuffData =
        StringArrayStuffData(listOf(state, badgeName, ownerName, displayedAt))

    fun toExtraData(): String =
        listOf(badgeName, ownerName, displayedAt).joinToString(FurnitureLogic.SEPARATOR.toString())

    companion object {
        fun parse(extraData: String): BadgeDisplayData {
            val split = extraData.split(FurnitureLogic.SEPARATOR)

            return BadgeDisplayData(
                badgeName = split.getOrElse(0) { "" },
                ownerName = split.getOrElse(1) { "" },
                displayedAt = split.getOrElse(2) { "" },
            )
        }
    }
}
