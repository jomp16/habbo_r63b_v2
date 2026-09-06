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
 * Gift / presente (interaction_type = gift).
 *
 * Correlação AS3: FurniturePresentLogic (com.sulake.habbo.room.object.logic.furniture),
 * keys do MapStuffData:
 *  - "EXTRA_PARAM"       -> sempre vazio no client
 *  - "MESSAGE"           -> mensagem da carta do presente
 *  - "PURCHASER_NAME"    -> nome de quem comprou (presente em presentes com remetente visível)
 *  - "PURCHASER_FIGURE"  -> figure de quem comprou
 *  - "TRUSTED_SENDER"    -> "true"/"false", marca remetente confiável (model "furniture_trusted_sender")
 *  - "PRODUCT_CODE"      -> código do produto do catálogo (model "furniture_type_id")
 *  - "state"             -> "1" quando o presente está sendo removido (magicRemove)
 *
 * O roomExtra do quarto é o openState calculado: openStateA * 1000 + openStateB
 * (animação de abertura do client).
 *
 * Formato no banco (extra_data): extraParam[SEP]message[SEP]openStateA[SEP]openStateB[SEP]
 * showPurchaser[SEP]purchaserName[SEP]purchaserFigure[SEP]productCode
 */
data class GiftData(
    val message: String,
    val productCode: String,
    val openStateA: Int = 0,
    val openStateB: Int = 0,
    val showPurchaser: Boolean = false,
    val purchaserName: String = "",
    val purchaserFigure: String = "",
    val extraParam: String = "",
    val trustedSender: String? = null,
) {
    val openState: Int
        get() = openStateA * 1000 + openStateB

    fun toStuffData(magicRemove: Boolean = false): MapStuffData {
        val values = linkedMapOf(
            "EXTRA_PARAM" to extraParam,
            "MESSAGE" to message,
        )

        if (showPurchaser) {
            values["PURCHASER_NAME"] = purchaserName
            values["PURCHASER_FIGURE"] = purchaserFigure
        }

        trustedSender?.let { values["TRUSTED_SENDER"] = it }

        values["PRODUCT_CODE"] = productCode
        values[StuffData.KEY_STATE] = if (magicRemove) "1" else "0"

        return MapStuffData(values, roomExtra = openState)
    }

    fun toExtraData(): String =
        listOf(
            extraParam,
            message,
            openStateA.toString(),
            openStateB.toString(),
            showPurchaser.toString(),
            purchaserName,
            purchaserFigure,
            productCode,
        ).joinToString(FurnitureLogic.SEPARATOR.toString())

    companion object {
        fun parse(extraData: String): GiftData {
            val split = extraData.split(FurnitureLogic.SEPARATOR)

            return GiftData(
                extraParam = split.getOrElse(0) { "" },
                message = split.getOrElse(1) { "" },
                openStateA = split.getOrElse(2) { "0" }.toIntOrNull() ?: 0,
                openStateB = split.getOrElse(3) { "0" }.toIntOrNull() ?: 0,
                showPurchaser = split.getOrElse(4) { "false" }.toBoolean(),
                purchaserName = split.getOrElse(5) { "" },
                purchaserFigure = split.getOrElse(6) { "" },
                productCode = split.getOrElse(7) { "" },
            )
        }
    }
}
