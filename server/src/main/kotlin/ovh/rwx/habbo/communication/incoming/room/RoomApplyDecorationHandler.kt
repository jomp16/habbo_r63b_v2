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
import ovh.rwx.habbo.communication.outgoing.room.RoomDecorationData
import ovh.rwx.habbo.game.item.InteractionType
import ovh.rwx.habbo.game.user.HabboSession
import kotlin.math.abs

@Suppress("unused", "UNUSED_PARAMETER")
class RoomApplyDecorationHandler {
    @Handler(Incoming.ROOM_APPLY_DECORATION)
    @HandlerR63A(IncomingR63A.ROOM_APPLY_DECORATION)
    fun handle(habboSession: HabboSession, habboRequest: HabboRequest) {
        val room = habboSession.currentRoom ?: return
        if (!room.userManager.hasRights(habboSession, ownerRight = true)) return

        val itemId = abs(habboRequest.readInt())
        val userItem = habboSession.habboInventory.items[itemId] ?: return

        if (userItem.furnishing.interactionType != InteractionType.ROOM_EFFECT) return

        val key = userItem.furnishing.itemName
        val value = userItem.extraData

        when (key) {
            "floor" -> {
                room.roomData.floor = value
                // ACH_RoomDecoFloor: mudar piso
                HabboServer.habboGame.achievementManager.progress(
                    habboSession,
                    "ACH_RoomDecoFloor",
                    1,
                    accumulate = true
                )
            }

            "wallpaper" -> {
                room.roomData.wallpaper = value
                // ACH_RoomDecoWallpaper: mudar papel de parede
                HabboServer.habboGame.achievementManager.progress(
                    habboSession,
                    "ACH_RoomDecoWallpaper",
                    1,
                    accumulate = true
                )
            }

            "landscape" -> {
                room.roomData.landscape = value
                // ACH_RoomDecoLandscape: mudar fundo
                HabboServer.habboGame.achievementManager.progress(
                    habboSession,
                    "ACH_RoomDecoLandscape",
                    1,
                    accumulate = true
                )
            }

            else -> return
        }

        room.roomData.markDirty()

        room.sendResponse(
            Outgoing.ROOM_DECORATION,
            OutgoingR63A.ROOM_DECORATION,
            RoomDecorationData(key, value)
        )

        habboSession.habboInventory.removeItems(listOf(itemId), true)
    }
}