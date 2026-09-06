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
 * Mannequin (interaction_type = mannequin).
 *
 * Correlação AS3: FurnitureMannequinLogic (com.sulake.habbo.room.object.logic.furniture),
 * keys do MapStuffData:
 *  - "GENDER"      -> figura masculina ("m") ou feminina ("f")
 *  - "FIGURE"      -> figure string do look guardado
 *  - "OUTFIT_NAME" -> nome do outfit, exibido no model "furniture_mannequin_name"
 *
 * Formato no banco (extra_data): gender[SEP]figure[outfitName]
 */
data class MannequinData(
    val gender: String,
    val figure: String,
    val outfitName: String,
) {
    fun toStuffData(): MapStuffData =
        MapStuffData(
            linkedMapOf(
                "GENDER" to gender,
                "FIGURE" to figure,
                "OUTFIT_NAME" to outfitName,
            )
        )

    fun toExtraData(): String =
        listOf(gender, figure, outfitName).joinToString(FurnitureLogic.SEPARATOR.toString())

    companion object {
        fun parse(extraData: String): MannequinData {
            val split = extraData.split(FurnitureLogic.SEPARATOR)

            return MannequinData(
                gender = split.getOrElse(0) { "" },
                figure = split.getOrElse(1) { "" },
                outfitName = split.getOrElse(2) { "" },
            )
        }
    }
}
