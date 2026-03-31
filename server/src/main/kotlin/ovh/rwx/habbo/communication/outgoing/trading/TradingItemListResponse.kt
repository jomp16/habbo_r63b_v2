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

package ovh.rwx.habbo.communication.outgoing.trading

import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.communication.HabboResponse
import ovh.rwx.habbo.communication.Response
import ovh.rwx.habbo.communication.ResponseR63A
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.OutgoingR63A
import ovh.rwx.habbo.game.item.ItemType
import ovh.rwx.habbo.game.item.user.UserItem

@Suppress("unused")
class TradingItemListResponse {
    @Response(Outgoing.TRADING_ITEM_LIST)
    fun response(
        habboResponse: HabboResponse,
        firstUserId: Int,
        firstUserItems: Collection<UserItem>,
        secondUserId: Int,
        secondUserItems: Collection<UserItem>
    ) {
        habboResponse.apply {
            // Primeiro usuário
            writeInt(firstUserId)
            writeInt(firstUserItems.size)
            firstUserItems.forEach { item ->
                writeUserItem(habboResponse, item)
            }
            writeInt(firstUserItems.size)
            writeInt(0) // firstUserNumCredits (R63B - créditos na troca)

            // Segundo usuário
            writeInt(secondUserId)
            writeInt(secondUserItems.size)
            secondUserItems.forEach { item ->
                writeUserItem(habboResponse, item)
            }
            writeInt(secondUserItems.size)
            writeInt(0) // secondUserNumCredits (R63B - créditos na troca)
        }
    }

    @ResponseR63A(OutgoingR63A.TRADING_ITEM_LIST)
    fun responseR63A(
        habboResponse: HabboResponse,
        firstUserId: Int,
        firstUserItems: Collection<UserItem>,
        secondUserId: Int,
        secondUserItems: Collection<UserItem>
    ) {
        habboResponse.apply {
            // Primeiro usuário
            writeInt(firstUserId)
            writeInt(firstUserItems.size)
            firstUserItems.forEach { item ->
                writeUserItemR63A(habboResponse, item)
            }

            // Segundo usuário
            writeInt(secondUserId)
            writeInt(secondUserItems.size)
            secondUserItems.forEach { item ->
                writeUserItemR63A(habboResponse, item)
            }
        }
    }

    private fun writeUserItem(habboResponse: HabboResponse, item: UserItem) {
        habboResponse.apply {
            writeInt(item.id)                    // itemId
            writeUTF(item.furnishing.type.type.uppercase())                   // itemType
            writeInt(0)                          // roomItemId (0 = inventário)
            writeInt(item.furnishing.spriteId)   // itemTypeId
            writeInt(0)                          // category
            writeBoolean(item.furnishing.allowInventoryStack) // groupable
            HabboServer.habboGame.itemManager.writeExtradata(
                habboResponse,
                item.extraData,
                item.furnishing,
                item.limitedItemData,
                inventory = true,
            )
            writeInt(0)                          // creationDay
            writeInt(0)                          // creationMonth
            writeInt(0)                          // creationYear

            // Se itemType == "S" (floor), escreve songID (apenas para música)
            if (item.furnishing.type == ItemType.FLOOR) {
                writeInt(-1) // songID (-1 = não é música)
            }
        }
    }

    /**
     * Helper para serializar um UserItem no formato do Trading R63A.
     */
    private fun writeUserItemR63A(habboResponse: HabboResponse, item: UserItem) {
        habboResponse.apply {
            writeInt(item.id)                    // itemID
            writeUTF(item.furnishing.type.type.uppercase())                    // itemType
            writeInt(0)                          // roomItemID (0 = inventário)
            writeInt(item.furnishing.spriteId)   // itemTypeID
            writeInt(0)                          // category (não usado)
            writeBoolean(item.furnishing.allowInventoryStack) // groupable
            writeUTF(item.extraData)
            writeInt(0)                          // creationDay
            writeInt(0)                          // creationMonth
            writeInt(0)                          // creationYear

            // Se itemType == "S" (floor), escreve songID (apenas para música)
            if (item.furnishing.type == ItemType.FLOOR) {
                writeInt(-1) // songID (-1 = não é música)
            }
        }
    }
}
