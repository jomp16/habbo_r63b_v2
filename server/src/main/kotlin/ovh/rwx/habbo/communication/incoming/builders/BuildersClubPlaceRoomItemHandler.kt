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

package ovh.rwx.habbo.communication.incoming.builders

import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.communication.HabboRequest
import ovh.rwx.habbo.communication.Handler
import ovh.rwx.habbo.communication.incoming.Incoming
import ovh.rwx.habbo.communication.outgoing.Outgoing
import ovh.rwx.habbo.communication.outgoing.builders.BuildersClubPlacementWarningResponse
import ovh.rwx.habbo.communication.outgoing.misc.MiscSuperNotificationResponse
import ovh.rwx.habbo.database.item.ItemDao
import ovh.rwx.habbo.database.item.ItemPurchaseData
import ovh.rwx.habbo.game.item.ItemType
import ovh.rwx.habbo.game.user.HabboSession
import ovh.rwx.habbo.util.Vector2

@Suppress("unused", "UNUSED_PARAMETER")
class BuildersClubPlaceRoomItemHandler {

    companion object {
        private fun sendFloorError(
            habboSession: HabboSession,
            pageId: Int,
            offerId: Int,
            extraParam: String,
            x: Int,
            y: Int,
            direction: Int
        ) {
            habboSession.sendHabboResponse(
                Outgoing.BUILDERS_PLACE_ITEM_WARNING,
                BuildersClubPlacementWarningResponse.BuildersClubPlacementData(
                    warningType = 0,
                    pageId = pageId,
                    offerId = offerId,
                    extraParam = extraParam,
                    x = x,
                    y = y,
                    direction = direction,
                    isWall = false
                )
            )
        }
    }

    @Handler(Incoming.BUILDERS_PLACE_ROOM_ITEM)
    fun handle(habboSession: HabboSession, habboRequest: HabboRequest) {
        val pageId = habboRequest.readInt()
        val offerId = habboRequest.readInt()
        val extraParam = habboRequest.readUTF()
        val x = habboRequest.readInt()
        val y = habboRequest.readInt()
        val direction = habboRequest.readInt()

        val accepted = if (habboRequest.byteBuf.readableBytes() > 0) {
            habboRequest.readBoolean() || habboSession.habboSubscription.buildersClubSubscription.itemsUsed > 0
        } else {
            true
        }

        val room = habboSession.currentRoom ?: return

        // Validacoes - retorna erro se falhar
        if (room.roomData.ownerId != habboSession.userInformation.id) {
            sendFloorError(habboSession, pageId, offerId, extraParam, x, y, direction)
            return
        }

        if (!habboSession.habboSubscription.hasBuildersClub && !accepted) {
            sendFloorError(habboSession, pageId, offerId, extraParam, x, y, direction)
            return
        }

        if (habboSession.habboSubscription.buildersClubSubscription.itemsUsed >= habboSession.habboSubscription.buildersClubSubscription.itemsLimit) {
            sendFloorError(habboSession, pageId, offerId, extraParam, x, y, direction)
            return
        }

        val catalogItem = HabboServer.habboGame.catalogManager.catalogItems.find { it.id == offerId }
        if (catalogItem == null) {
            sendFloorError(habboSession, pageId, offerId, extraParam, x, y, direction)
            return
        }

        val furnishing = catalogItem.furnishing
        if (furnishing.type != ItemType.FLOOR) {
            sendFloorError(habboSession, pageId, offerId, extraParam, x, y, direction)
            return
        }

        val correctExtraData =
            HabboServer.habboGame.itemManager.correctExtradataCatalog(habboSession, extraParam, furnishing)
                ?: return

        // Se preview=true, validacoes passaram, nao envia warning (sucesso silencioso)
        // Se preview=false, teoricamente nao deveria colocar o item, mas o Habbo coloca direto
        // apos receber confirmacao do usuario

        val userItems = ItemDao.addItems(
            habboSession.userInformation.id,
            listOf(ItemPurchaseData(furnishing, correctExtraData, limited = false, buildersClub = true))
        )

        if (userItems.isEmpty()) {
            sendFloorError(habboSession, pageId, offerId, extraParam, x, y, direction)
            return
        }

        val userItem = userItems.first()

        val roomItem = HabboServer.habboGame.itemManager.getRoomItemFromUserItem(
            room.roomData.id,
            userItem
        )

        val success = room.setFloorItem(roomItem, Vector2(x, y), direction, habboSession.roomUser)

        if (!success) {
            ItemDao.deleteItems(listOf(userItem.id))
            sendFloorError(habboSession, pageId, offerId, extraParam, x, y, direction)
            return
        }

        if (habboSession.habboSubscription.buildersClubSubscription.trial && habboSession.habboSubscription.buildersClubSubscription.itemsUsed == 0) {
            habboSession.sendSuperNotification(MiscSuperNotificationResponse.MiscSuperNotificationKeys.BUILDERS_CLUB_ROOM_LOCKED)
        }

        habboSession.habboSubscription.incrementBuildersItemsUsed()

        // Sucesso - NAO envia BUILDERS_PLACE_ITEM_WARNING!
        // Apenas atualiza o contador
        habboSession.sendHabboResponse(Outgoing.BUILDERS_FURNI_COUNT, habboSession.habboSubscription)
    }
}
