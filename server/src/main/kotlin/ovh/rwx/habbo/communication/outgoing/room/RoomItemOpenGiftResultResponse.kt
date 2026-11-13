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

package ovh.rwx.habbo.communication.outgoing.room

import ovh.rwx.habbo.communication.HabboResponse
import ovh.rwx.habbo.communication.Response
import ovh.rwx.habbo.communication.ResponseR63A
import ovh.rwx.habbo.communication.isVersionAtLeast
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.OutgoingR63A
import ovh.rwx.habbo.game.item.Furnishing

/**
 * PresentOpened. [furnishing] é o primeiro conteúdo; [placedItemId] é o id do conteúdo (o MESMO id que ele já tem no
 * quarto, se [placedInRoom], ou no inventário).
 */
data class GiftOpenedData(
    val furnishing: Furnishing,
    val placedItemId: Int,
    val placedInRoom: Boolean,
)

/**
 * Layout por era (parser AS3 conferido nas builds):
 * - antes de 2011-11-08: String itemType, int classId, String productCode (productCode só desde 2011-03-30);
 * - 2011-11-08 (RELEASE63-201111082302): + int placedItemId, String placedItemType, Boolean placedInRoom
 *   (de 2011-11-23 a 2012-01-19 há um petFigure lido SÓ quando placedItemType == "p", que nunca enviamos);
 * - 2012-01-20 (RELEASE63-201201201105) em diante: + String petFigure incondicional ("").
 */
@Suppress("unused", "UNUSED_PARAMETER")
class RoomItemOpenGiftResultResponse {
    @Response(Outgoing.ROOM_ITEM_OPEN_GIFT_RESULT)
    @ResponseR63A(OutgoingR63A.ROOM_ITEM_OPEN_GIFT_RESULT)
    fun response(habboResponse: HabboResponse, data: GiftOpenedData) {
        habboResponse.apply {
            val furnishing = data.furnishing

            writeUTF(furnishing.type.type)
            writeInt(furnishing.spriteId)

            if (isVersionAtLeast(2011, 3, 30)) {
                writeUTF(furnishing.itemName)
            }

            if (isVersionAtLeast(2011, 11, 8)) {
                writeInt(data.placedItemId)
                writeUTF(furnishing.type.type)
                writeBoolean(data.placedInRoom)
            }

            if (isVersionAtLeast(2012, 1, 20)) {
                writeUTF("") // petFigure
            }
        }
    }
}
