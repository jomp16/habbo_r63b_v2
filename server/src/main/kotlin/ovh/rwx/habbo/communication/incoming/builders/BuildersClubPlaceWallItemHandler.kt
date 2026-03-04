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
import ovh.rwx.habbo.database.item.ItemDao
import ovh.rwx.habbo.database.item.ItemPurchaseData
import ovh.rwx.habbo.game.item.ItemType
import ovh.rwx.habbo.game.user.HabboSession

@Suppress("unused", "UNUSED_PARAMETER")
class BuildersClubPlaceWallItemHandler {

    companion object {
        private fun sendWallError(
            habboSession: HabboSession,
            pageId: Int,
            offerId: Int,
            extraParam: String,
            wallLocation: String
        ) {
            habboSession.sendHabboResponse(
                Outgoing.BUILDERS_PLACE_ITEM_WARNING,
                BuildersClubPlacementWarningResponse.BuildersClubPlacementData(
                    warningType = 1,
                    pageId = pageId,
                    offerId = offerId,
                    extraParam = extraParam,
                    wallLocation = wallLocation,
                    isWall = true
                )
            )
        }
    }

    @Handler(Incoming.BUILDERS_PLACE_WALL_ITEM)
    fun handle(habboSession: HabboSession, habboRequest: HabboRequest) {
        val pageId = habboRequest.readInt()
        val offerId = habboRequest.readInt()
        val extraParam = habboRequest.readUTF()
        val wallLocation = habboRequest.readUTF()
        val accepted = if (habboRequest.byteBuf.readableBytes() > 0) {
            habboRequest.readBoolean() || habboSession.habboSubscription.buildersClubSubscription.itemsUsed > 0
        } else {
            true
        }

        val room = habboSession.currentRoom ?: return

        if (room.roomData.ownerId != habboSession.userInformation.id) {
            sendWallError(habboSession, pageId, offerId, extraParam, wallLocation)
            return
        }

        if (!habboSession.habboSubscription.hasBuildersClub && !accepted) {
            sendWallError(habboSession, pageId, offerId, extraParam, wallLocation)
            return
        }

        if (habboSession.habboSubscription.buildersClubSubscription.itemsUsed >= habboSession.habboSubscription.buildersClubSubscription.itemsLimit) {
            sendWallError(habboSession, pageId, offerId, extraParam, wallLocation)
            return
        }

        val catalogItem = HabboServer.habboGame.catalogManager.catalogItems.find { it.id == offerId }
        if (catalogItem == null) {
            sendWallError(habboSession, pageId, offerId, extraParam, wallLocation)
            return
        }

        val furnishing = catalogItem.furnishing
        if (furnishing.type != ItemType.WALL) {
            sendWallError(habboSession, pageId, offerId, extraParam, wallLocation)
            return
        }

        val correctExtraData =
            HabboServer.habboGame.itemManager.correctExtradataCatalog(habboSession, extraParam, furnishing)
                ?: return

        val userItems = ItemDao.addItems(
            habboSession.userInformation.id,
            listOf(ItemPurchaseData(furnishing, correctExtraData, limited = false, buildersClub = true))
        )

        if (userItems.isEmpty()) {
            sendWallError(habboSession, pageId, offerId, extraParam, wallLocation)
            return
        }

        val userItem = userItems.first()

        val roomItem = HabboServer.habboGame.itemManager.getRoomItemFromUserItem(
            room.roomData.id,
            userItem.copy(buildersClub = true)
        )
        roomItem.wallPosition = wallLocation

        val correctedWallData = wallLocation.split(' ')
        val success = room.setWallItem(roomItem, correctedWallData, habboSession.roomUser)

        if (!success) {
            ItemDao.deleteItems(listOf(userItem.id))
            sendWallError(habboSession, pageId, offerId, extraParam, wallLocation)
            return
        }

        // Sucesso
        habboSession.habboSubscription.incrementBuildersItemsUsed(room)
    }
}
