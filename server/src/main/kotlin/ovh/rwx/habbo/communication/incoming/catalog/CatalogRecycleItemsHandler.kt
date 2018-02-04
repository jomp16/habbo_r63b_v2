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

package ovh.rwx.habbo.communication.incoming.catalog

import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.communication.HabboRequest
import ovh.rwx.habbo.communication.Handler
import ovh.rwx.habbo.communication.incoming.Incoming
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.catalog.CatalogRecycleItemsResultResponse
import ovh.rwx.habbo.database.item.ItemDao
import ovh.rwx.habbo.game.user.HabboSession
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Suppress("unused", "UNUSED_PARAMETER")
class CatalogRecycleItemsHandler {
    @Handler(Incoming.CATALOG_RECYCLE_ITEMS)
    fun handle(habboSession: HabboSession, habboRequest: HabboRequest) {
        if (!habboSession.authenticated || habboSession.currentRoom == null) return
        val amount = habboRequest.readInt()
        val ecotronBoxFurnishing = HabboServer.habboGame.itemManager.furnishings[HabboServer.habboConfig.recyclerConfig.giftBox]
        val recycledItemReward = HabboServer.habboGame.catalogManager.getRandomRecyclerReward()

        if (amount != HabboServer.habboConfig.recyclerConfig.slots || ecotronBoxFurnishing == null || recycledItemReward == null) {
            habboSession.sendHabboResponse(Outgoing.CATALOG_RECYCLE_ITEMS_RESULT, CatalogRecycleItemsResultResponse.CatalogRecycleItemsResult.FAILURE, 0)

            return
        }
        val itemIds = mutableListOf<Int>()

        repeat(amount) { itemIds += habboRequest.readInt() }

        habboSession.habboInventory.removeItems(itemIds, true)
        val userItem = ItemDao.addGiftItem(habboSession.userInformation.id, ecotronBoxFurnishing, 1, LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), recycledItemReward, "")

        habboSession.habboInventory.addItems(listOf(userItem))

        habboSession.sendHabboResponse(Outgoing.CATALOG_RECYCLE_ITEMS_RESULT, CatalogRecycleItemsResultResponse.CatalogRecycleItemsResult.SUCCESS, userItem.id)
    }
}