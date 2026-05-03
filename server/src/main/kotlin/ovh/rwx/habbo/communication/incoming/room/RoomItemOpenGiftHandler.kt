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

package ovh.rwx.habbo.communication.incoming.room

import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.communication.HabboRequest
import ovh.rwx.habbo.communication.Handler
import ovh.rwx.habbo.communication.HandlerR63A
import ovh.rwx.habbo.communication.incoming.Incoming
import ovh.rwx.habbo.communication.incoming.IncomingR63A
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.OutgoingR63A
import ovh.rwx.habbo.database.item.ItemDao
import ovh.rwx.habbo.database.item.ItemPurchaseData
import ovh.rwx.habbo.game.item.ItemType
import ovh.rwx.habbo.game.item.room.RoomItem
import ovh.rwx.habbo.game.user.HabboSession
import java.util.concurrent.TimeUnit

@Suppress("unused", "UNUSED_PARAMETER")
class RoomItemOpenGiftHandler {
    @Handler(Incoming.ROOM_ITEM_OPEN_GIFT)
    @HandlerR63A(IncomingR63A.ROOM_ITEM_OPEN_GIFT)
    fun handle(habboSession: HabboSession, habboRequest: HabboRequest) {
        if (habboSession.currentRoom == null) return
        val giftItemId = habboRequest.readInt()
        val giftRoomItem = habboSession.currentRoom!!.itemManager.items[giftItemId]

        if (giftRoomItem == null || giftRoomItem.userId != habboSession.userInformation.id) return

        if (giftRoomItem.furnishing.itemName == HabboServer.habboConfig.recyclerConfig.giftBox) {
            // recycler box, doesn't need magic remove, open box right now
            openBox(habboSession, giftRoomItem)
        } else {
            // normal gift, do a magic remove and then open box
            giftRoomItem.magicRemove = true
            giftRoomItem.update(updateDb = false, updateClient = true)

            HabboServer.serverScheduledExecutor.schedule({ openBox(habboSession, giftRoomItem) }, 3, TimeUnit.SECONDS)
        }
    }

    private fun openBox(habboSession: HabboSession, giftRoomItem: RoomItem) {
        val giftData = ItemDao.getGiftData(giftRoomItem.id) ?: return
        val shouldAddToRoom = giftData.furnishing.type == ItemType.FLOOR && giftData.amount == 1

        // Remove o item presente da room
        habboSession.currentRoom!!.itemManager.removeItem(habboSession.roomUser, giftRoomItem)

        // Deleta o registro do presente do banco
        ItemDao.deleteItems(listOf(giftRoomItem.id))
        ItemDao.deleteGiftData(giftData.id)

        var openedRoomItemId = 0

        if (shouldAddToRoom) {
            // Cria um NOVO item com NOVO ID no banco e na room
            val itemPurchaseDatas = listOf(
                ItemPurchaseData(
                    giftData.furnishing,
                giftData.extradata,
                giftData.limited,
                buildersClub = false
                )
            )

            val newUserItems = ItemDao.addItems(habboSession.userInformation.id, itemPurchaseDatas)
            val newItem = newUserItems.firstOrNull() ?: return

            val roomItem = HabboServer.habboGame.itemManager.getRoomItemFromUserItem(
                habboSession.currentRoom!!.roomData.id,
                newItem
            )
            
            habboSession.currentRoom!!.itemManager.setFloorItem(
                roomItem,
                giftRoomItem.position.vector2,
                giftRoomItem.rotation,
                habboSession.roomUser
            )

            openedRoomItemId = roomItem.id
        } else {
            // Adiciona itens ao inventário do usuário
            val itemPurchaseDatas = mutableListOf<ItemPurchaseData>()

            repeat(giftData.amount) {
                itemPurchaseDatas += ItemPurchaseData(
                    giftData.furnishing,
                    giftData.extradata,
                    giftData.limited,
                    buildersClub = false
                )
            }

            habboSession.habboInventory.addItems(ItemDao.addItems(habboSession.userInformation.id, itemPurchaseDatas))
        }

        // Envia resposta para o cliente
        if (habboSession.release == "R63A") {
            habboSession.sendHabboResponse(OutgoingR63A.ROOM_ITEM_OPEN_GIFT_RESULT, giftData.furnishing)
        } else {
            habboSession.sendHabboResponse(
                Outgoing.ROOM_ITEM_OPEN_GIFT_RESULT,
                openedRoomItemId,
                giftData.extradata,
                giftData.furnishing,
                shouldAddToRoom
            )
        }
    }
}