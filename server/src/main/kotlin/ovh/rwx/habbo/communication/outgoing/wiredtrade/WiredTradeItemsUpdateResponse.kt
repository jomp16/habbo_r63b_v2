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

package ovh.rwx.habbo.communication.outgoing.wiredtrade

import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.communication.HabboResponse
import ovh.rwx.habbo.communication.Response
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.game.chest.ChestData
import ovh.rwx.habbo.game.chest.ChestType
import ovh.rwx.habbo.game.item.ItemType
import ovh.rwx.habbo.game.item.user.UserItem
import ovh.rwx.habbo.game.user.HabboSession

/**
 * Espelho de _SafeStr_2980 (_-HO): estado da Wired Trade.
 *
 * TradingItems (_SafeStr_3339): firstUserID, [count + TradingItem...],
 * firstUserNumItems, firstUserNumCredits, secondUserID, [count + TradingItem...],
 * secondUserNumItems, secondUserNumCredits.
 * No client: FIRST = "você" (_ownUserItems), SECOND = lado do baú (_wiredItems).
 *
 * TradingItem (_SafeStr_2482): itemId:I, itemType:S ("S" floor / "I" wall),
 * roomItemId:I, itemTypeId:I (sprite), category:I, rented:B, stuffData,
 * creationDay:I, creationMonth:I, creationYear:I, [extra:I se "S"].
 */
@Suppress("unused", "UNUSED_PARAMETER")
class WiredTradeItemsUpdateResponse {
    @Response(Outgoing.WIRED_TRADE_ITEMS_UPDATE)
    fun response(
        habboResponse: HabboResponse,
        habboSession: HabboSession,
        chest: ChestData,
        ownItems: List<UserItem>,
        ownCredits: Int,
        canAccept: Boolean
    ) {
        habboResponse.apply {
            // TradingItems
            writeInt(habboSession.userInformation.id) // firstUserID = você

            writeInt(ownItems.size)
            ownItems.forEach { serializeTradingItem(this, it) }

            writeInt(ownItems.size)   // firstUserNumItems
            writeInt(ownCredits)      // firstUserNumCredits (câmbios no coin chest)

            writeInt(chest.itemId) // secondUserID = o baú

            writeInt(0)            // second: sem itens na devolução
            writeInt(0)            // secondUserNumItems
            writeInt(if (chest.type == ChestType.COINS) chest.coins else 0)

            writeBoolean(canAccept)
            writeInt(0)            // extra
        }
    }

    private fun serializeTradingItem(habboResponse: HabboResponse, item: UserItem) {
        val furnishing = item.furnishing
        val isWall = furnishing.type == ItemType.WALL

        habboResponse.apply {
            writeInt(item.id)
            writeUTF(if (isWall) "I" else "S")
            writeInt(-1) // roomItemId (inventário)
            writeInt(furnishing.spriteId)
            writeInt(1)  // category
            writeBoolean(false) // rented

            HabboServer.habboGame.itemManager
                .getFurnitureLogic(furnishing)
                .parseStuffData(item.extraData, furnishing, item.limitedItemData)
                .writeFull(this)

            writeInt(0) // creationDay
            writeInt(0) // creationMonth
            writeInt(0) // creationYear

            if (!isWall) writeInt(0) // extra
        }
    }
}
