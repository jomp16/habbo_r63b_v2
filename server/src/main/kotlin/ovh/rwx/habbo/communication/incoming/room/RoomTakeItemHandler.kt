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
import ovh.rwx.habbo.database.item.ItemDao
import ovh.rwx.habbo.game.item.InteractionType
import ovh.rwx.habbo.game.user.HabboSession

@Suppress("unused", "UNUSED_PARAMETER")
class RoomTakeItemHandler {
    @Handler(Incoming.ROOM_TAKE_ITEM)
    @HandlerR63A(IncomingR63A.ROOM_TAKE_ITEM)
    fun handle(habboSession: HabboSession, habboRequest: HabboRequest) {
        if (habboSession.currentRoom == null) return

        habboSession.currentRoom?.let { room ->
            if (!room.hasRights(habboSession)) return

            habboRequest.readInt() // useless
            val itemId = habboRequest.readInt()
            val roomItem = room.roomItems[itemId] ?: return

            if (roomItem.furnishing.interactionType == InteractionType.POST_IT) return

            // Itens BC: apenas o dono do quarto pode remover
            if (roomItem.buildersClub) {
                if (room.roomData.ownerId != habboSession.userInformation.id) {
                    return
                }
                if (room.removeItem(habboSession.roomUser, roomItem)) {
                    ItemDao.deleteItems(listOf(roomItem.id))
                    habboSession.habboSubscription.decrementBuildersItemsUsed(room)
                }
            } else {
                if (room.removeItem(habboSession.roomUser, roomItem)) {
                    ItemDao.addRoomItemInventory(mutableListOf(roomItem))
                }
            }

            // Por mover, girar, escolher ou colocar Mobis nos seus quartos.
            HabboServer.habboGame.achievementManager.progress(habboSession, "ACH_Tutorial5", 1, false)
        }
    }
}