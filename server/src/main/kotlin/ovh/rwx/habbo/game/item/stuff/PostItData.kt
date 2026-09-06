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
 * Post-it / stickie (interaction_type = postit).
 *
 * Correlação AS3: FurnitureStickieLogic (com.sulake.habbo.room.object.logic.furniture),
 * cores válidas no client (setColorIndexFromItemData):
 *  ["9CCEFF", "FF9CFF", "9CFF9C", "FFFF33", "FFFFFF", "FF9C9C", "FFCC66", "9CFFFF"]
 * (índice inválido cai no 3 = "FFFF33").
 *
 * O extra data é LegacyStuffData (FORMAT_KEY 0): a cor é o prefixo e o restante
 * é o texto da mensagem.
 *
 * Formato no banco (extra_data): color SPACE text
 */
data class PostItData(
    val color: String,
    val text: String,
) {
    fun toStuffData(): LegacyStuffData =
        LegacyStuffData(toExtraData())

    fun toExtraData(): String =
        if (text.isEmpty()) color else "$color $text"

    companion object {
        const val DEFAULT_COLOR = "FFFF33"

        fun parse(extraData: String): PostItData {
            val spaceIndex = extraData.indexOf(' ')

            return if (spaceIndex == -1) {
                PostItData(color = extraData.ifBlank { DEFAULT_COLOR }, text = "")
            } else {
                PostItData(
                    color = extraData.substring(0, spaceIndex),
                    text = extraData.substring(spaceIndex + 1),
                )
            }
        }
    }
}
