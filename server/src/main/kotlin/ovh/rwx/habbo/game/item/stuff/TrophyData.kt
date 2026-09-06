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

/**
 * Troféu (interaction_type = trophy).
 *
 * Correlação AS3: FurnitureTrophyLogic (com.sulake.habbo.room.object.logic.furniture)
 * abre o widget de troféu; o extra data é LegacyStuffData (FORMAT_KEY 0) com os
 * campos separados por TAB (char 9): owner username, data e mensagem gravada.
 *
 * Formato no banco (extra_data): ownerName<TAB>date<TAB>message
 */
data class TrophyData(
    val ownerName: String,
    val date: String,
    val message: String,
) {
    private val separator = 9.toChar()

    fun toStuffData(): LegacyStuffData =
        LegacyStuffData(toExtraData())

    fun toExtraData(): String =
        listOf(ownerName, date, message).joinToString(separator.toString())

    companion object {
        fun parse(extraData: String): TrophyData {
            val split = extraData.split(9.toChar())

            return TrophyData(
                ownerName = split.getOrElse(0) { "" },
                date = split.getOrElse(1) { "" },
                message = split.getOrElse(2) { "" },
            )
        }
    }
}
